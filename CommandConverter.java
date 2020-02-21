package com.common;

import com.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;

/**
 * 命令处理器
 */
@Slf4j
public class CommandConverter {

    private SdjInfoService sdjInfoService;

    public final static String COMAND_ING = "14 01"; // 心跳命令控制码
    public final static String COMAND_ER_CODE = "1F 37"; // 二维码控制码
    public final static String COMAND_OUT_STATUS = "14 36";  // 出货结果 控制码

    public final static String COMAND_ING_RETURN = "94 01"; // 返回 心跳命令控制码
    public final static String COMAND_ER_CODE_RETURN = "9F 37"; // 二维码回复控制码
    public final static String COMAND_ER_CODE_OUT_RETURN = "14 34"; // 出货 控制码
    public final static String COMAND_UPDATE_STATUS_RETURN = "94 36";  // 状态更新控制码

    public final static String END_STRING = "00 00 FF"; // 字符串结尾

    public CommandConverter() {
        this.sdjInfoService = SpringContextUtils.getBean(SdjInfoService.class);
    }

    public String verifyCommand(String content)  throws Exception {
        String returnContent = "";
        // 获取指令code
        String command = subCommandIng(content);
        if (command.startsWith(COMAND_ING)) {
            // 心跳连接
            returnContent = content.replace(COMAND_ING, COMAND_ING_RETURN);

        } else if (command.startsWith(COMAND_ER_CODE)) {
            // 二维码获取
            returnContent =  erCodeCommand(content);

        } else if (command.startsWith(COMAND_OUT_STATUS)) {
            // 更新订单状态是否出货
            returnContent = updateOrderStatus(content);
        }
        return returnContent;
    }

    /**
     * 订单出货命令 *订单号*保留位*保留位*货道编号*状态
     * @param content
     * @return
     */
    public String updateOrderStatus(String content)  throws Exception {
        String data = null;
        try {
            String dataPlace = content.substring(content.indexOf("2A") + 2, content.indexOf(END_STRING)).trim();
            data = convertHexToString(dataPlace);
        } catch (Exception e) {
            log.error("订单出货命令指令截取失败：content.substring(content.indexOf(\"2A\") + 2, content.lastIndexOf(\"2A\") + 5)", e);
            return "";
        }
        String[] dataArr = data.trim().split("\\*");
        if (dataArr == null || dataArr.length < 5) {
            log.error("订单出货状态更新失败：" + data);
            return "";
        }

        String openid = dataArr[0];
        int status = parseInt(dataArr[4]);

        int num = sdjInfoService.updateSdjInfo(openid, status + "");
        if (num <= 0) {
            log.info("更新出货成功数据失败openid: " + openid + "， 状态： " + status);
        }
        return content.replace(COMAND_OUT_STATUS, COMAND_UPDATE_STATUS_RETURN);
    }

    /**
     * 获取二维码指令 1F 37
     * @param content
     * @return
     */
    public String erCodeCommand(String content)  throws Exception {
        // 示例：FA 71 F7 91 B5 01 5D E8 65 20 1F 37  00 0A 31 32 33 34 35 36 37 38 39 30  00 00 FF
        int start = content.indexOf(COMAND_ER_CODE);
        try {
            String data = content.substring(start + 12, content.length() - 8);
            String openid = convertHexToString(data.trim());
            if (StringUtils.isNotEmpty(openid)) {
                SdjInfo sdjInfo = sdjInfoService.querySdjInfoByOpenid(openid);
                if (sdjInfoService.checkOrderOut(sdjInfo)) {
                    // 订单出货命令 *订单号*货道编号*数量
                    int huogui = sdjInfo.getJp();
                    int cishu = sdjInfo.getCishu();
                    String huoguiStr = copyString(huogui + "", 2);
                    String cishuStr = copyString(cishu + "", 3);

                    String hexHuogui = convertStringToHex(huoguiStr);
                    String hexCishu = convertHexToString(cishuStr);

                    String backStr = content.substring(0, content.indexOf(END_STRING));
                    // 控制码变更为出货控制码
                    backStr.replace(COMAND_ER_CODE, COMAND_ER_CODE_OUT_RETURN);
                    backStr = backStr.trim() + " 2A " + hexHuogui + " 2A " + hexCishu + " " + END_STRING;
                    // 更新订单数据状态为出货中
                    int num = sdjInfoService.updateSdjInfo(openid, SdjInfo.ING_OUT);
                    if (num <= 0) {
                        log.info("准备出货数据状态更新失败");
                    }
                    return backStr;
                }
            }
            // 控制码变更为二维码回复控制码
            return content.replace(COMAND_ER_CODE, COMAND_ER_CODE_RETURN);
        } catch (Exception e) {
            log.error("二维码解析，回复命令解析失败", e);
            return "";
        }
    }

    /**
     * 获取 指令 （公共）
     * @param content
     * @return
     */
    public String subCommandIng(String content)  throws Exception {
        //  示例 ： FA 71 F7 91 B5 01 5D E8 65 20 14 01 00 03 2A 33 31 00 00 FF
        int start = 30; // 指令在30
        try {
            return content.substring(start, start + 5).trim();
        } catch (Exception e) {
            log.error("公共指令截取失败：content.substring(start, start + 5)", e);
            return "";
        }
    }

    // 十六进制 转ascill码
    public static String convertHexToString(String hex) {

        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();
        String[] hexArr = hex.split(" ");
        for(String output : hexArr){
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char)decimal);
            temp.append(decimal);
        }
        return sb.toString();
    }

    // ascill码 转16进制
    public static String convertStringToHex(String str) {

        char[] chars = str.toCharArray();

        StringBuffer hex = new StringBuffer();
        for(int i = 0; i < chars.length; i++){
            hex.append(Integer.toHexString((int)chars[i])).append(" ");
        }

        return hex.toString();
    }

    // 转 int 类型
    public int parseInt(String data) throws Exception{
        BigDecimal bigDecimal = new BigDecimal(data);
        return bigDecimal.intValue();

    }

    /**
     * 补全字符串
     * @param content 需要补全的内容
     * @param end 补全的位数
     * @return
     */
    public String copyString(String content, int end) {
        if (content.length() < end) {
            int copyLen = 3 - content.length();
            StringBuffer copyStr = new StringBuffer();
            for (int i = 0; i < copyLen; i++) {
                copyStr.append("0");
            }
            content = copyStr.toString() + content;
        }
        return content;
    }
}
