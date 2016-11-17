package com.mx.cttic.card;

import java.util.Map;

import android.app.Activity;

import com.mx.httpclient.ArteryPreClient;
import com.mx.httpclient.ArteryPreDef;

public class CtticUtil {
	public static final int QUERY_RATE_ERROR = 1;
	public static final int SUCCESS = 0;
	// public static final String CTTIC_MERID = "200005";
	// public static final String CTTIC_MERID_EP = "200001";
	public static final String CTTIC_MERID = "200101";
	public static final String CTTIC_MERID_EP = "200101";
	public static final String CUP_PAYID = "300101";
	public static final String ALIPAY_PAYID = "100003";
	public static final String CMB_PAYID = "";

	public interface RequestReatCallBack {
		public void onReceiveCallBack(int code, CtticRate rates);
	}

	public static void getRate(final RequestReatCallBack requestReatCallBack, final Activity activity) {
		if (requestReatCallBack == null) {
			return;
		}
		new Thread(new Runnable() {
			@Override
			public void run() {

				String payId = CMB_PAYID;

				Map<String, Object> rate = ArteryPreClient.getInstance().queryRate(CTTIC_MERID, payId);
				if ((Integer) rate.get(ArteryPreDef.FIELD_result) != 0) {
					// 直接失败了
					requestReatCallBack.onReceiveCallBack(QUERY_RATE_ERROR, null);
				} else {
					if (rate.get("rateType").equals("1")) {
						// 按比例
						CtticRate ctticRate = new CtticRate();
						ctticRate.setRateType(RateType.RateByPercentage);
						ctticRate.setData(rate.get("rate").toString());
						requestReatCallBack.onReceiveCallBack(QUERY_RATE_ERROR, ctticRate);
					} else {
						// 固定笔数费率
						CtticRate ctticRate = new CtticRate();
						ctticRate.setRateType(RateType.RateByCount);
						ctticRate.setData(rate.get("rate").toString());
						requestReatCallBack.onReceiveCallBack(SUCCESS, ctticRate);
					}
				}
			}
		}).start();
	}

	public static class CtticRate {
		RateType rateType;
		String data;

		public RateType getRateType() {
			return rateType;
		}

		public void setRateType(RateType rateType) {
			this.rateType = rateType;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}

		@Override
		public String toString() {
			return "CtticRate [rateType=" + rateType + ", data=" + data + "]";
		}
	}

	public enum RateType {
		RateByPercentage, RateByCount
	}
}
