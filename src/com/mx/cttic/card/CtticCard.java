package com.mx.cttic.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.R.integer;

import com.cttic.se.ConnectException;
import com.cttic.se.CtticReader;
import com.cttic.se.TimeoutException;
import com.mx.nfclib.NFCReader;
import com.mx.se.CardDef;
import com.mx.util.MXBaseUtil;
import com.mx.util.exception.DeviceConnectException;
import com.mx.util.exception.DeviceTransException;
import com.mx.util.iso7816.Iso7816Impl.ReaderTag;
import com.mx.util.pboc.PbocEntity;
import com.mx.util.pboc.PbocOffLineResp;

public class CtticCard {
	public static final String AID_MOT_EC = "A000000632010106";
	public static final String AID_MOT_EP = "A000000632010105";
	public static final String AID_MOT_COMMON = "A00000000386980701";
	public static final String AID_MOT_BJ = "4F43";
	public static final String AID_MOT_SZ = "5041592E535A54";
	public static final String AID_MOT_JL = "A00000000886980701";

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
					CtticCardInfo ctticCardInfo = new CtticCardInfo();
					String aidString = CtticCard.AID_MOT_EP;
					int sw = readerTag.selectByName(MXBaseUtil.hex2byte(aidString));
					while (true) {

						if (sw != CardDef.SWOK) {
							aidString = CtticCard.AID_MOT_COMMON;
							sw = readerTag.selectByName(MXBaseUtil.hex2byte(aidString));
						} else {
							sw = readerTag.readBinary(0x15);
							if (sw != CardDef.SWOK) {
								mQueryCardCallBack.onReceiveQueryCard(CtticTradeResult.READBINARY_ERROR, null);
								return;
							}

							ctticCardInfo.setCardId(readerTag.getRes().substring(21, 40));
							ctticCardInfo.setStartDate(readerTag.getRes().substring(40, 48));
							ctticCardInfo.setEndDate(readerTag.getRes().substring(48, 56));

							ctticCardInfo.setType("互联互通");

							sw = readerTag.sendAPDU("805C010204");
							if (sw == CardDef.SWOK) {
								ctticCardInfo.setLimitBalance(String.format("%d",
										Integer.parseInt(readerTag.getRes(), 16)));
							}
							sw = readerTag.sendAPDU("805C020204");
							if (sw == CardDef.SWOK) {
								ctticCardInfo.setUsedLimitAmt(String.format("%d",
										Integer.parseInt(readerTag.getRes(), 16)));
							}
							sw = readerTag.sendAPDU("805C030204");
							if (sw == CardDef.SWOK) {
								ctticCardInfo.setUseBalance(String.format("%d",
										Integer.parseInt(readerTag.getRes(), 16)));
							}

							break;
						}

						if (sw != CardDef.SWOK) {
							aidString = CtticCard.AID_MOT_BJ;
							sw = readerTag.selectByName(MXBaseUtil.hex2byte(aidString));
						} else {
							sw = readerTag.readBinary(0x15);
							if (sw != CardDef.SWOK) {
								mQueryCardCallBack.onReceiveQueryCard(CtticTradeResult.READBINARY_ERROR, null);
								return;
							}

							String cardId = readerTag.getRes().substring(20, 40);
							String startDate = readerTag.getRes().substring(40, 48);
							String endDate = readerTag.getRes().substring(48, 56);

							String cityCode = cardId.substring(0, 4);
							// 上海一卡通
							if (cityCode.equals("2000")) {
								String code = cardId.substring(12);
								long lCode = Long.valueOf(code, 16);
								String temp = String.valueOf(lCode);
								StringBuilder cardNoTemp = new StringBuilder();
								for (int i = 0; i < temp.length() / 2; i++) {
									cardNoTemp.append(temp.substring(temp.length() - (i + 1) * 2, temp.length() - i * 2));
								}

								ctticCardInfo.setCardId(cardNoTemp.toString());
								ctticCardInfo.setType("上海一卡通");
							}

							// 合肥通
							if (cityCode.equals("2300") || cityCode.equals("0000")) {
								ctticCardInfo.setCardId(cardId.substring(12));
								ctticCardInfo.setType("合肥通");
							}

							// 杭州一卡通
							if (cityCode.equals("3100")) {
								ctticCardInfo.setCardId(cardId.substring(12));
								ctticCardInfo.setType("杭州一卡通");
							}

							// 哈尔滨城市通
							if (cityCode.equals("1500")) {
								ctticCardInfo.setCardId(String.valueOf(Integer.valueOf(cardId.substring(12), 16)));
								ctticCardInfo.setType("哈尔滨城市通");
							}

							// 大连明珠卡
							if (cityCode.equals("1160")) {
								ctticCardInfo.setCardId(String.valueOf(Integer.valueOf(cardId.substring(12), 16)));
								ctticCardInfo.setType("大连明珠卡");
							}
							
							if (cityCode.equals("1231")) {
								ctticCardInfo.setCardId(String.valueOf(Integer.valueOf(cardId.substring(12), 16)));
								ctticCardInfo.setType("重庆畅通卡");
							}

							ctticCardInfo.setStartDate(startDate);
							ctticCardInfo.setEndDate(endDate);

							sw = readerTag.sendAPDU("805C000204");
							if (sw == CardDef.SWOK) {

								long nlBalance = Long.valueOf(readerTag.getRes(), 16);
								long sub = Long.valueOf("80000000", 16);

								String balanceString = null;
								if (nlBalance > 1000000) {
									int i = (int) (nlBalance - sub);
									balanceString = String.valueOf(i);
								} else {
									balanceString = String.valueOf(nlBalance);
								}

								ctticCardInfo.setUseBalance(balanceString);
							}

							break;
						}

						if (sw != CardDef.SWOK) {
							aidString = CtticCard.AID_MOT_JL;
							sw = readerTag.selectByName(MXBaseUtil.hex2byte(aidString));
						} else {
							sw = readerTag.sendAPDU("805C000204");
							if (sw == CardDef.SWOK) {

								long nlBalance = Long.valueOf(readerTag.getRes(), 16);
								long sub = Long.valueOf("80000000", 16);

								String balanceString = null;
								if (nlBalance > 1000000) {
									int i = (int) (nlBalance - sub);
									balanceString = String.valueOf(i);
								} else {
									balanceString = String.valueOf(nlBalance);
								}

								ctticCardInfo.setUseBalance(balanceString);
							}

							sw = readerTag.selectByID(MXBaseUtil.hex2byte("3F00"));
							if (sw != CardDef.SWOK) {
								mQueryCardCallBack.onReceiveQueryCard(CtticTradeResult.SELECT_ERROR, null);
								return;
							}
							sw = readerTag.readBinary(0x04);
							String cardId = readerTag.getRes().substring(0, 16);
							ctticCardInfo.setCardId(cardId);
							ctticCardInfo.setType("北京一卡通");

							break;
						}

						if (sw != CardDef.SWOK) {
							aidString = CtticCard.AID_MOT_SZ;
							sw = readerTag.selectByName(MXBaseUtil.hex2byte(aidString));
						} else {
							sw = readerTag.readBinary(0x15);
							if (sw != CardDef.SWOK) {
								mQueryCardCallBack.onReceiveQueryCard(CtticTradeResult.READBINARY_ERROR, null);
								return;
							}

							String cardId = readerTag.getRes().substring(20, 40);
							String startDate = readerTag.getRes().substring(40, 48);
							String endDate = readerTag.getRes().substring(48, 56);

							ctticCardInfo.setCardId(cardId.substring(4));
							ctticCardInfo.setType("吉林通");
							ctticCardInfo.setStartDate(startDate);
							ctticCardInfo.setEndDate(endDate);

							sw = readerTag.sendAPDU("805C000204");
							if (sw == CardDef.SWOK) {

								long nlBalance = Long.valueOf(readerTag.getRes(), 16);
								long sub = Long.valueOf("80000000", 16);

								String balanceString = null;
								if (nlBalance > 1000000) {
									int i = (int) (nlBalance - sub);
									balanceString = String.valueOf(i);
								} else {
									balanceString = String.valueOf(nlBalance);
								}

								ctticCardInfo.setUseBalance(balanceString);
							}

							break;
						}

						if (sw != CardDef.SWOK) {
							mQueryCardCallBack.onReceiveQueryCard(CtticTradeResult.SELECT_ERROR, null);
							return;
						} else {
							sw = readerTag.readBinary(0x15);
							if (sw != CardDef.SWOK) {
								mQueryCardCallBack.onReceiveQueryCard(CtticTradeResult.READBINARY_ERROR, null);
								return;
							}

							String cardId = readerTag.getRes().substring(20, 40);
							String startDate = readerTag.getRes().substring(40, 48);
							String endDate = readerTag.getRes().substring(48, 56);

							String temp = cardId.substring(12);
							StringBuilder cardNoTemp = new StringBuilder();
							for (int i = 0; i < temp.length() / 2; i++) {
								cardNoTemp.append(temp.substring(temp.length() - (i+1) * 2, temp.length() - i * 2));
							}
							
							String cardNoString = cardNoTemp.toString();

							ctticCardInfo.setCardId(String.valueOf(Integer.valueOf(cardNoString, 16)));
							ctticCardInfo.setType("深圳通");
							ctticCardInfo.setStartDate(startDate);
							ctticCardInfo.setEndDate(endDate);

							sw = readerTag.sendAPDU("805C000204");
							if (sw == CardDef.SWOK) {

								long nlBalance = Long.valueOf(readerTag.getRes(), 16);
								long sub = Long.valueOf("80000000", 16);

								String balanceString = null;
								if (nlBalance > 1000000) {
									int i = (int) (nlBalance - sub);
									balanceString = String.valueOf(i);
								} else {
									balanceString = String.valueOf(nlBalance);
								}

								ctticCardInfo.setUseBalance(balanceString);
							}

							break;
						}
					}

					// ep log
					sw = readerTag.selectByName(MXBaseUtil.hex2byte(aidString));
					if (sw != CardDef.SWOK) {
						mQueryCardCallBack.onReceiveQueryCard(CtticTradeResult.SELECT_ERROR, null);
						return;
					}

					List<String> epLog = new ArrayList<String>();
					for (int i = 1; i <= 10; i++) {
						sw = readerTag.readRecord(0x18, i);
						if (sw == CardDef.SWOK) {
							if (readerTag.getRes().contains("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")) {
								continue;
							}
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

					if (pbocOffLineResp.getCode() != 0) {
						mQueryPbocCardCallBack.onReceiveQueryPBCard(CtticTradeResult.SELECT_ERROR, null);
						mCtticReader.powerOff();
						return;
					}

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
					} else {
						mQueryPbocCardCallBack.onReceiveQueryPBCard(CtticTradeResult.READBINARY_ERROR, null);
						mCtticReader.powerOff();
						return;
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

		sw = readerTag.selectByName(MXBaseUtil.hex2byte(CtticCard.AID_MOT_COMMON));
		if (sw == CardDef.SWOK)
			return true;

		sw = readerTag.selectByName(MXBaseUtil.hex2byte(CtticCard.AID_MOT_BJ));
		if (sw == CardDef.SWOK)
			return true;

		sw = readerTag.selectByName(MXBaseUtil.hex2byte(CtticCard.AID_MOT_JL));
		if (sw == CardDef.SWOK)
			return true;

		sw = readerTag.selectByName(MXBaseUtil.hex2byte(CtticCard.AID_MOT_SZ));
		if (sw == CardDef.SWOK)
			return true;

		return false;

	}
}
