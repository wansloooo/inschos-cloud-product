package com.inschos.proposal.controller.action;

import com.inschos.proposal.controller.bean.BaseResponse;
import com.inschos.proposal.controller.bean.TradingCallBackBean;
import com.inschos.proposal.kit.*;
import com.inschos.proposal.model.CustWarranty;
import com.inschos.proposal.service.CustWarrantyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by IceAnt on 2018/3/27.
 */
@Component
public class TradingAction extends BaseAction {

    @Autowired
    private CustWarrantyService custWarrantyService;

    public String callBack(String body) {
        L.log.info("pay call back body :" + body);

        TradingCallBackBean.PayCallBackRequest request = XmlKit.xml2Bean(body, TradingCallBackBean.PayCallBackRequest.class);

        L.log.info("pay call back body to json :" + JsonKit.bean2Json(request));

        if (verifySign(request)) {
            L.log.info("pay call back success");

            CustWarranty custWarranty = custWarrantyService.findByProPolicyNo(request.mainDto.proposalNo);
            if(custWarranty!=null){
                if(!StringKit.isEmpty(custWarranty.warranty_code)){
                    custWarranty.warranty_code += ","+request.mainDto.policyNo;
                }else{
                    custWarranty.warranty_code = request.mainDto.policyNo;
                }
                if(CustWarranty.WARRANTY_STATUS_BAOZHANGZHONG!=custWarranty.warranty_status){
                    if("4".equals(request.mainDto.status)){
                        custWarranty.pay_status = CustWarranty.PAY_STATUS_SUCCESS;
                        custWarranty.warranty_status = CustWarranty.WARRANTY_STATUS_BAOZHANGZHONG;
                    }else{
                        custWarranty.pay_status = CustWarranty.PAY_STATUS_FAILED;
                        custWarranty.warranty_status = CustWarranty.WARRANTY_STATUS_DAIZHIFU;
                    }
                    custWarranty.resp_pay_msg = toMsg(StringKit.isEmpty(request.mainDto.status)?request.head.transType:request.mainDto.status);
                }
                custWarrantyService.changeWarrantyInfo(custWarranty);
            }

            return json(BaseResponse.CODE_SUCCESS, "回调成功");
        } else {
            L.log.error("pay call back failed : verify sign data failed");
            L.log.error("pay call back body :" + body);
            return json(BaseResponse.CODE_FAILED, "签名验证失败");
        }

    }

    private String toMsg(String status){

        String msg = "";
        if(StringKit.isInteger(status)){
            switch (status){
                case "01":
                    msg = "生成保单失败";
                    break;
                case "02":
                    msg = "生成保单成功";
                    break;
                case "3":
                    msg = "缴费成功未生成保单";
                    break;
                case "4":
                    msg = "生成保单";
                    break;
                case "5":
                    msg = "缴费失败";
                    break;

            }
        }
        return msg;
    }


    private boolean verifySign(TradingCallBackBean.PayCallBackRequest request) {
        boolean isSuccess = false;
        if (request != null && request.head != null && request.mainDto != null) {
            String oldSign = request.mainDto.signDate;
            String sign = Md5Util.getMD5String(request.head.requestType + request.mainDto.proposalNo + request.mainDto.status + request.head.requestType);
            isSuccess = sign.equals(oldSign);
        }
        return isSuccess;
    }
}
