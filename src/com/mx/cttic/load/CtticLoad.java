package com.mx.cttic.load;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Looper;

import com.mx.cttic.card.CtticCard;
import com.mx.cttic.card.CtticCard.DeviceType;
import com.mx.cttic.card.CtticTradeResult;
import com.mx.httpclient.ArteryPreClient;
import com.mx.httpclient.ArteryPreDef;
import com.mx.httpclient.ArteryPreTrans;
import com.mx.nfclib.NFCReader;
import com.cttic.se.CtticReader;
import com.cttic.se.ConnectException;
import com.cttic.se.TimeoutException;
import com.mx.util.MXLog;
import com.mx.util.PayOrder;
import com.mx.util.iso7816.Iso7816Impl;

public class CtticLoad {
	private static CtticLoad load;
	static final String TAG = "CtticLoad";
	static final String CID = "01";
	static final String merId = "200100";
	static final String cmb_channel = "100101";

	private Thread mLoadThread;

	private String mAmount;
	private String mDeviceId;
	private String mUserId;

	private CtticReader mCtticReader;
	private DeviceType mDeviceType;
	private LOAD_TYPE mLoadType;
	private PayOrder mOrder;

	private CtticTradeResult mCtticTradeResult;

	private MXApplyOrderCallBack mApplyOrderCallBack;
	private MXLoadCallBack mLoadCallBack;
	private Map<String, Object> resultMap;
	private Map<String, Object> paramesField;
	private Map<String, Object> preField;
	private Map<String, Object> allData;

	public enum LOAD_TYPE {
		LOCAL, INTERFLOW;
	}

	public static CtticLoad getInstance() {
		if (load == null) {
			load = new CtticLoad();

		}
		return load;
	}

	public void registerCtticReader(CtticReader ctticReader) {
		mCtticReader = ctticReader;
	}

	public void applyCMBPayOrder(final String amount, final String deviceId, final String userId,
			MXApplyOrderCallBack callBack) {
		this.mApplyOrderCallBack = callBack;
		this.mAmount = amount;
		this.mDeviceId = deviceId;
		this.mUserId = userId;

		mLoadThread = new Thread(new ApplyTradeIdThread());
		mLoadThread.start();
	}

	public void doCtticLoad(final PayOrder order, DeviceType deviceType, LOAD_TYPE loadType, MXLoadCallBack callback) {
		this.mLoadCallBack = callback;
		this.mDeviceType = deviceType;
		this.mOrder = order;
		this.mLoadType = loadType;

		mLoadCallBack.onMessageOut("检测卡片状态");
		CtticCard.getInstance(deviceType).registerCtticReader(mCtticReader);
		if (!CtticCard.getInstance(deviceType).checkCard())
			doFailCallBack(CtticTradeResult.DEVICE_POWER_ON_ERROR, "卡片上电失败");

		ExecutorService executor = Executors.newCachedThreadPool();
		Future<Boolean> result = executor.submit(new PayConfirmThread());

		try {
			MXLog.i(TAG, mDeviceType.name());
			if (result.get()) {

				if (mDeviceType == DeviceType.NFC) {
					mCtticReader = NFCReader.getInstance();
				}

				mLoadThread = new Thread(new LoadForCttic());
				mLoadThread.start();

			}
			executor.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}

	private class ApplyTradeIdThread implements Runnable {

		@Override
		public void run() {
			Looper.prepare();

			allData = new HashMap<String, Object>();
			paramesField = new HashMap<String, Object>();
			preField = new HashMap<String, Object>();
			paramesField.put(ArteryPreDef.FIELD_tradeAmount, mAmount);
			paramesField.put(ArteryPreDef.FIELD_deviceId, mDeviceId);
			paramesField.put("cid", CID);
			preField.put("merId", merId);
			ArteryPreClient arteryPreCilent = ArteryPreClient.getInstance();
			resultMap = arteryPreCilent.tradeInit(paramesField, preField);
			MXLog.i(TAG, resultMap.toString());
			allData.putAll(resultMap);

			int resultCode = Integer.valueOf(allData.get("result").toString());
			if (0 != resultCode) {
				ApplyOrderResult result = new ApplyOrderResult();
				result.setResultCode(resultCode);
				result.setResultDesc(allData.get("resultDesc").toString());
				mApplyOrderCallBack.onReceiveCallBack(result);

				Looper.loop();

				return;
			}

			String tranTime = allData.get("tranTime").toString();
			String tradeId = allData.get("tradeId").toString();

			HashMap<String, String> payExtraInfo = new HashMap<String, String>();
			payExtraInfo.put(ArteryPreDef.FIELD_userId, mUserId);

			paramesField.put(ArteryPreDef.FIELD_tradeId, tradeId);
			paramesField.put(ArteryPreDef.FIELD_tradeAmount, mAmount);
			paramesField.put(ArteryPreDef.FIELD_channel, cmb_channel);

			paramesField.put(ArteryPreDef.FIELD_payExtraInfo, payExtraInfo);

			resultMap = arteryPreCilent.payInit(paramesField, preField);
			MXLog.i(TAG, resultMap.toString());
			allData.putAll(resultMap);

			ApplyOrderResult result = new ApplyOrderResult();

			resultCode = Integer.valueOf(allData.get("result").toString());
			if (0 == resultCode) {
				result.setResultCode(resultCode);

				PayOrder order = new PayOrder();
				order.setOrderNo(allData.get("orderNO").toString());
				order.setOrderInfo(allData.get("orderInfo").toString());
				order.setOrderTime(tranTime);
				order.setTradeId(tradeId);
				order.setAmount(mAmount);
				order.setOrderChannel(PayOrder.OrderChannel.CMB);
				result.setOrder(order);
			} else {
				result.setResultCode(resultCode);
				result.setResultDesc(allData.get("resultDesc").toString());
			}

			mApplyOrderCallBack.onReceiveCallBack(result);

			Looper.loop();

		}

	}

	public class PayConfirmThread implements Callable<Boolean> {
		@Override
		public Boolean call() {
			paramesField.clear();
			preField.clear();
			paramesField.put(ArteryPreDef.FIELD_tradeId, mOrder.getTradeId());
			
			paramesField.put(ArteryPreDef.FIELD_retryTime, "-1");
			mLoadCallBack.onMessageOut("正在查询支付结果");
			while (true) {
				resultMap = ArteryPreTrans.getInstance().queryPayStatus(paramesField, preField);
				allData.putAll(resultMap);
				if ((Integer) resultMap.get(ArteryPreDef.FIELD_result) != 0) {
					doFailCallBack((Integer) resultMap.get(ArteryPreDef.FIELD_result),
							(String) resultMap.get(ArteryPreDef.FIELD_resultDesc));
					return false;
				} else {
					if (resultMap.containsKey(ArteryPreDef.FIELD_payResult)) {
						String isPaySuccess = (String) resultMap.get(ArteryPreDef.FIELD_payResult);
						if (isPaySuccess.equals("success")) {
							break;
						} else if (isPaySuccess.equals("failed")) {
							doFailCallBack((Integer) resultMap.get(ArteryPreDef.FIELD_result),
									(String) resultMap.get(ArteryPreDef.FIELD_resultDesc));
							return false;
						} else if (isPaySuccess.equals("suspensive")) {
							String interval = (String) resultMap.get(ArteryPreDef.FIELD_interval);
							String retryTime = (String) resultMap.get(ArteryPreDef.FIELD_retryTime);
							paramesField.put(ArteryPreDef.FIELD_retryTime,
									String.valueOf(Integer.parseInt(retryTime) - 1));
							try {
								Thread.sleep(Integer.parseInt(interval) * 1000);
							} catch (NumberFormatException e) {
								e.printStackTrace();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						} else {
							doFailCallBack((Integer) resultMap.get(ArteryPreDef.FIELD_result),
									(String) resultMap.get(ArteryPreDef.FIELD_resultDesc));
							return false;
						}

					} else {
						doFailCallBack((Integer) resultMap.get(ArteryPreDef.FIELD_result),
								(String) resultMap.get(ArteryPreDef.FIELD_resultDesc));
						return false;
					}
				}
			}
			return true;
		}

	}

	public class LoadForCttic implements Runnable {

		@Override
		public void run() {
			mLoadCallBack.onMessageOut("开始充值");
			Iso7816Impl.ReaderTag readerTag = new Iso7816Impl.ReaderTag(mCtticReader);
			try {
				mCtticReader.powerOn(1000);
			} catch (ConnectException e) {
				e.printStackTrace();
				doFailCallBack(CtticTradeResult.DEVICE_POWER_ON_ERROR, "卡片上电失败");
				return;
			} catch (TimeoutException e) {
				e.printStackTrace();
				doFailCallBack(CtticTradeResult.DEVICE_POWER_ON_ERROR, "卡片上电超时");
				return;
			}
			paramesField.clear();
			preField.clear();

			paramesField.put(ArteryPreDef.FIELD_tradeId, mOrder.getTradeId());
			paramesField.put("inApdu", "");
			paramesField.put("step", "");
			if (mLoadType == LOAD_TYPE.INTERFLOW) {
				paramesField.put("area", "1"); 
			} else if (mLoadType == LOAD_TYPE.LOCAL) {
				paramesField.put("area", "2"); 
			}

			preField.put(ArteryPreDef.FIELD_method, "");

			ArteryPreClient arteryPreCilent = ArteryPreClient.getInstance();

			Map<String, Object> result = arteryPreCilent.merchantDo(paramesField, preField);

			while (true) {
				MXLog.d(TAG, result.toString());
				if ((Integer) result.get(ArteryPreDef.FIELD_result) != 0) {
					doFailCallBack((Integer) result.get(ArteryPreDef.FIELD_result),
							(String) result.get(ArteryPreDef.FIELD_resultDesc));
					break;
				}
				try {
					JSONObject responseApdu = new JSONObject();
					JSONArray respListArray = new JSONArray();
					boolean isContinue = true;
					if (result.get("outAPDU") != null && !result.get("outAPDU").equals("")) {
						JSONObject outApdu = new JSONObject((String) result.get("outAPDU"));
						JSONArray apduList = outApdu.getJSONArray("APDU");
						for (int i = 0; i < apduList.length(); i++) {
							JSONObject jsonObject = new JSONObject(apduList.getString(i));
							String apduData = jsonObject.getString("APDUDATA");
							String dataFlag = jsonObject.getString("DATAFLAG");
							String retsw = jsonObject.getString("RETSW");
							int sw = 0x7FFFFFFF;
							if (isContinue) {
								sw = readerTag.sendAPDU(apduData);
								JSONObject response = new JSONObject();
								if (sw != Integer.valueOf(retsw, 16)) {
									isContinue = false;
								}
								response.put("APDUDATA", apduData);
								response.put("APDUSW", Integer.toHexString(sw));
								if (dataFlag.endsWith("01")) {
									response.put("RETDATA", readerTag.getRes());
								} else {
									response.put("RETDATA", "");
								}
								respListArray.put(response);
							} else {
								JSONObject response = new JSONObject();
								response.put("APDUDATA", apduData);
								response.put("APDUSW", null);
								response.put("RETDATA", "");
								respListArray.put(response);
							}
						}
						responseApdu.put("APDU", respListArray.toString());
						responseApdu.put("APDUSUM", String.valueOf(respListArray.length()));
						MXLog.d(TAG, apduList.toString());
						MXLog.d(TAG, outApdu.toString());
					}

					// String lastApdu = (String) result.get("LASTAPDU");
					String method = (String) result.get("method");
					if (method.equalsIgnoreCase("$END$")) {
						if (mCtticTradeResult == null) {
							mCtticTradeResult = new CtticTradeResult();
						}
						mCtticTradeResult.setResultCode(0);
						mCtticTradeResult.setTradeId(mOrder.getTradeId());
						mCtticTradeResult.setTradeAmt(mOrder.getAmount());
						mLoadCallBack.onReceiveCallBack(mCtticTradeResult);
						return;
					}
					String step = (String) result.get("step");
					paramesField.clear();
					paramesField.put(ArteryPreDef.FIELD_tradeId, mOrder.getTradeId());
					paramesField.put("inAPDU", responseApdu.toString());
					paramesField.put("step", step);

					if (mLoadType == LOAD_TYPE.INTERFLOW) {
						paramesField.put("area", "1");// 1:互联互通
					} else if (mLoadType == LOAD_TYPE.LOCAL) {
						paramesField.put("area", "2");// 1:本地应用
					}

					preField.clear();
					preField.put(ArteryPreDef.FIELD_method, method);
					result = arteryPreCilent.merchantDo(paramesField, preField);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

	}

	private void doFailCallBack(int code, String errInfo) {
		if (mCtticTradeResult == null) {
			mCtticTradeResult = new CtticTradeResult();
		}
		mCtticTradeResult.setTradeId((String) allData.get(ArteryPreDef.FIELD_tradeId));
		mCtticTradeResult.setResultCode(code);
		mCtticTradeResult.setErrInfo(errInfo);
		mLoadCallBack.onReceiveCallBack(mCtticTradeResult);
	}

	public interface MXApplyOrderCallBack {
		public void onReceiveCallBack(ApplyOrderResult result);
	}

	public interface MXLoadCallBack {
		public void onReceiveCallBack(CtticTradeResult result);

		public void onMessageOut(String msg);
	}
}
