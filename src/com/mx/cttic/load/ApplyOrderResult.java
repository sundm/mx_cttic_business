package com.mx.cttic.load;

import com.mx.util.PayOrder;

public class ApplyOrderResult {
	private int resultCode;
	private String resultDesc;
	private PayOrder order;

	public int getResultCode() {
		return resultCode;
	}

	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}

	public String getResultDesc() {
		return resultDesc;
	}

	public void setResultDesc(String resultDesc) {
		this.resultDesc = resultDesc;
	}

	public PayOrder getOrder() {
		return order;
	}

	public void setOrder(PayOrder order) {
		this.order = order;
	}

	@Override
	public String toString() {
		return "ApplyOrderResult [resultCode=" + resultCode + ", resultDesc=" + resultDesc + ", order=" + order + "]";
	}

}
