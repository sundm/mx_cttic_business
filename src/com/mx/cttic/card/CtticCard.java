package com.mx.cttic.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.R.integer;

import com.mx.nfclib.NFCReader;
import com.cttic.se.CtticReader;
import com.mx.se.CardDef;
import com.cttic.se.ConnectException;
import com.cttic.se.TimeoutException;
import com.mx.util.MXBaseUtil;
import com.mx.util.exception.DeviceConnectException;
import com.mx.util.exception.DeviceTransException;
import com.mx.util.iso7816.Iso7816Impl.ReaderTag;
import com.mx.util.pboc.PbocEntity;
import com.mx.util.pboc.PbocOffLineResp;

public class CtticCard {
	public static final String AID_MOT_EC = "A000000632010106";
	public static final String AID_MOT_EP = "A000000632010105";

	public static final String AID_MOT_PBOC = "A0000003330101";

	public static final String TERM_ID = "313131313131";
	public static final int SUCCESS = 0;
	public static final int CODE_BUSY = 1;
	private static final String TAG = "CtticCard";

	QueryCardCallBack mQueryCardCallBack;
	QueryPbocCardCallBack mQueryPbocCardCallBack;
	CardLoadCallBack mCardLoadCallBack;
	QueryTradeStatusCallBack mQueryTradeStatusCallBack;
	CtticReader mCtticReader;
	DeviceType mDeviceType;
	private static CtticCard mCtticCard;

	private CtticCard(DeviceType deviceType) {
		mDeviceType = deviceType;
	}

	public static CtticCard getInstance(DeviceType deviceType) {
		if (mCtticCard == null) {
			synchronized (CtticCard.class) {
				if (mCtticCard == null) {
					mCtticCard = new CtticCard(deviceType);
				}
			}
		}
		if (deviceType != mCtticCard.mDeviceType) {
			mCtticCard.mDeviceType = deviceType;
		}
		return mCtticCard;
	}

	public String getVersion() {
		return "1.0.0";
	}

	public void registerCtticReader(CtticReader ctticReader) {
		mCtticReader = ctticReader;
	}

	public void requestQueryCard(QueryCardCallBack callBack) {

		mQueryCardCallBack = callBack;

		new Thread(new Runnable() {

			@Override
			public void run() {
				if (mDeviceType == DeviceType.NFC) {
					if (checkCard()) {
						mCtticReader = NFCReader.getInstance();
					} else {
						mQueryCardCallBack.onReceiveQueryCard(CtticTradeResult.CONNECT_ERROR, null);
						return;
					}
				}

				if (mCtticReader == null) {
					mQueryCardCallBack.onReceiveQueryCard(CtticTradeResult.CONNECT_ERROR, null);
					return;
				}

				try {
					mCtticReader.powerOn(2000);
					ReaderTag readerTag = new ReaderTag(mCtticReader);
					int sw = readerTag.selectByName(MXBaseUtil.hex2byte(CtticCard.AID_MOT_EP));
					if (sw != CardDef.SWOK) {
						mQueryCardCallBack.onReceiveQueryCard(CtticTradeResult.SELECT_ERROR, null);
						return;
					}
					sw = readerTag.readBinary(0x15);
					if (sw != CardDef.SWOK) {
						mQueryCardCallBack.onReceiveQueryCard(CtticTradeResult.READBINARY_ERROR, null);
						return;
					}
					CtticCardInfo ctticCardInfo = new CtticCardInfo();
					ctticCardInfo.setCardId(readerTag.getRes().substring(21, 40));
					ctticCardInfo.setStartDate(readerTag.getRes().substring(40, 48));
					ctticCardInfo.setEndDate(readerTag.getRes().substring(48, 56));
					// ���������棰�
					sw = readerTag.sendAPDU("805C010204");
					if (sw == CardDef.SWOK) {
						ctticCardInfo.setLimitBalance(String.format("%d", Integer.parseInt(readerTag.getRes(), 16)));
					}
					sw = readerTag.sendAPDU("805C020204");
					if (sw == CardDef.SWOK) {
						ctticCardInfo.setUsedLimitAmt(String.format("%d", Integer.parseInt(readerTag.getRes(), 16)));
					}
					sw = readerTag.sendAPDU("805C030204");
					if (sw == CardDef.SWOK) {
						ctticCardInfo.setUseBalance(String.format("%d", Integer.parseInt(readerTag.getRes(), 16)));
					}
					// ep log
					List<String> epLog = new ArrayList<String>();
					for (int i = 1; i <= 10; i++) {
						sw = readerTag.readRecord(0x18, i);
						if (sw == CardDef.SWOK) {
							epLog.add(readerTag.getRes());
						}
					}
					ctticCardInfo.setListCardEPRecords(epLog);

					PbocEntity pbocEntity = new PbocEntity();
					pbocEntity.setAid(AID_MOT_EC);
					List<Map<String, String>> logMap = null;
					logMap = pbocEntity.getOfflineLog(readerTag);
					ctticCardInfo.setListCardECRecords(logMap);
					mQueryCardCallBack.onReceiveQueryCard(SUCCESS, ctticCardInfo);

					// PbocOffLineResp pbocOffLineResp = pbocEntity
					// .getOffLineMap(readerTag);
					// if (pbocOffLineResp.getCode() == CardDef.CARD_SUCCESS) {
					// Map<String, String> pbocMap = pbocOffLineResp
					// .getPbocMap();
					// Log.i("map", pbocMap.toString());
					// }
				} catch (ConnectException e) {
					e.printStackTrace();
					mQueryCardCallBack.onReceiveQueryCard(CtticTradeResult.CONNECT_ERROR, null);
				} catch (TimeoutException e) {
					e.printStackTrace();
					mQueryCardCallBack.onReceiveQueryCard(CtticTradeResult.TIMEOUT_ERROR, null);
				} catch (DeviceTransException e) {
					e.printStackTrace();
					mQueryCardCallBack.onReceiveQueryCard(CtticTradeResult.TRANSPORT_ERROR, null);
				} finally {
					mCtticReader.powerOff();
				}
			}
		}).start();
	}

	public void requestQueryPbocCard(QueryPbocCardCallBack callBack) {

		mQueryPbocCardCallBack = callBack;
		new Thread(new Runnable() {

			@Override
			public void run() {
				if (mDeviceType == DeviceType.NFC) {
					if (checkCard()) {
						mCtticReader = NFCReader.getInstance();
					} else {
						mQueryPbocCardCallBack.onReceiveQueryPBCard(CtticTradeResult.CONNECT_ERROR, null);
						return;
					}
				}
				try {
					mCtticReader.powerOn(2000);
					ReaderTag readerTag = new ReaderTag(mCtticReader);

					PbocEntity pbocEntity = new PbocEntity();
					pbocEntity.setAid(AID_MOT_PBOC);

					PbocOffLineResp pbocOffLineResp = pbocEntity.getOffLineMap(readerTag);
					String balanceStr = pbocEntity.getData(readerTag, (byte) 0x9F, (byte) 0x79);

					List<Map<String, String>> logMap = pbocEntity.getOfflineLog(readerTag);

					if (pbocOffLineResp.getCode() == CardDef.CARD_SUCCESS) {
						Map<String, String> pbocMap = pbocOffLineResp.getPbocMap();
						String pan = pbocMap.get("5A").replaceAll("f|F", "");
						String startDate = pbocMap.get("5F25");
						String endDate = pbocMap.get("5F24");

						PbocCardInfo pbCardInfo = new PbocCardInfo();
						pbCardInfo.setBalance(balanceStr);
						pbCardInfo.setPan(pan);
						pbCardInfo.setStartDate(startDate);
						pbCardInfo.setEndDate(endDate);
						pbCardInfo.setListCardECRecords(logMap);

						mQueryPbocCardCallBack.onReceiveQueryPBCard(SUCCESS, pbCardInfo);
					}
				} catch (ConnectException e) {
					e.printStackTrace();
					mQueryPbocCardCallBack.onReceiveQueryPBCard(CtticTradeResult.CONNECT_ERROR, null);
				} catch (TimeoutException e) {
					e.printStackTrace();
					mQueryPbocCardCallBack.onReceiveQueryPBCard(CtticTradeResult.TIMEOUT_ERROR, null);
				} catch (DeviceTransException e) {
					e.printStackTrace();
					mQueryPbocCardCallBack.onReceiveQueryPBCard(CtticTradeResult.TRANSPORT_ERROR, null);
				} catch (DeviceConnectException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					mQueryPbocCardCallBack.onReceiveQueryPBCard(CtticTradeResult.CONNECT_ERROR, null);
				} finally {
					mCtticReader.powerOff();
				}
			}
		}).start();
	}

	public interface QueryCardCallBack {
		public void onReceiveQueryCard(int code, CtticCardInfo ctticCardInfo);
	}

	public interface QueryPbocCardCallBack {
		public void onReceiveQueryPBCard(int code, PbocCardInfo pbCardInfo);

	}

	public interface CardLoadCallBack {
		public void onReceiveCardLoad(int code, String errInfo, CtticTradeResult ctticTradeResult);

		public void onMessageOut(String message);
	}

	public interface QueryTradeStatusCallBack {
		public void onReceiveQueryTradeStatus(int code, String errInfo, Map<String, integer> tradeResultList);

		public void onMessageOut(String message);
	}

	public enum DeviceType {
		BLE, NFC;
	}

	public boolean checkCard() {
		CtticReader reader = null;
		if (mDeviceType == DeviceType.NFC) {
			reader = NFCReader.getInstance();

		} else if (mDeviceType == DeviceType.BLE) {
			reader = mCtticReader;
		}

		if (reader == null)
			return false;

		try {
			reader.powerOn(1000);
		} catch (ConnectException e) {
			e.printStackTrace();
			return false;
		} catch (TimeoutException e) {
			e.printStackTrace();
			return false;
		}

		ReaderTag readerTag = new ReaderTag(reader);
		int sw = readerTag.selectByName(MXBaseUtil.hex2byte(CtticCard.AID_MOT_EC));
		if (sw == CardDef.SWOK)
			return true;

		sw = readerTag.selectByName(MXBaseUtil.hex2byte(CtticCard.AID_MOT_EP));
		if (sw == CardDef.SWOK)
			return true;

		sw = readerTag.selectByName(MXBaseUtil.hex2byte(CtticCard.AID_MOT_PBOC));
		if (sw == CardDef.SWOK)
			return true;

		return false;

	}
}
