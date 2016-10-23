package com.zhouyuming.sync;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * Created by ZhouYuming on 2016/7/11.
 * 通信发送端口号为21212，接受端口号为发送端口号+1（21213）
 */
public class UDPSender {

	static private DatagramPacket datagramPacketCheck;
	static private DatagramSocket datagramSocketCheck;
	static private DatagramPacket datagramPacket;
	static private DatagramSocket datagramSocket;
	static private boolean checked;		//发送的消息是否收到ACK

	private static  final int ACK_SIZE = 50;
	private static  final int TIME_OUT = 500;

	/**
	 * 设定发送地址，并将设置写入数据库
	 * @param ipAddr ip地址
	 * @param context 上下文
	 */
	public static void setAddr(String ipAddr, Context context){
		DBHelper dbHelper = new DBHelper();
		dbHelper.init(context);
		dbHelper.open();
		dbHelper.setAddr(ipAddr);
		dbHelper.close();
	}

	/**
	 * 设定发送端口号，并将设置写入数据库
	 * @param port 端口号
	 * @param context 上下文
	 */
	public static void setPort(int port, Context context){
		DBHelper dbHelper = new DBHelper();
		dbHelper.init(context);
		dbHelper.open();
		dbHelper.setPort(port);
		dbHelper.close();
	}

	/**
	 * 向指定地址发送一个消息，异步执行
	 * @param msg 消息体
	 * @param checkCode 响应码
	 * @param context 上下文
	 * @return 如果得到响应就返回true，否则返回false，如果处于未连接状态则不发送直接返回false
	 */
	public static boolean sendWithCheck(final String msg, final String checkCode, Context context){

		DBHelper dbHelper = new DBHelper();
		dbHelper.init(context);
		dbHelper.open();
		boolean connected = dbHelper.isConnected();
		final InetAddress IPAddress = dbHelper.getIPAddr();
		final int port = dbHelper.getPort();
		dbHelper.close();

		if(!connected){
			Log.i("info", "disconnected!");
			return false;
		}

		checked = false;
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					datagramSocket = new DatagramSocket();
					datagramPacket = new DatagramPacket(msg.getBytes(), msg.getBytes().length, IPAddress, port);
					datagramSocket.send(datagramPacket);
					Log.i("info", "send to " + IPAddress + ":" + port + " msg:" + msg);

					byte[] check = new byte[ACK_SIZE];
					int checkport = port + 1;
					datagramSocketCheck = new DatagramSocket(checkport);
					datagramPacketCheck = new DatagramPacket(check, check.length);

					//等待时间TIME_OUT秒
					datagramSocket.setSoTimeout(TIME_OUT);
					datagramSocket.receive(datagramPacketCheck);
					String checkcodeRes = new String(datagramPacketCheck.getData(), 0, datagramPacketCheck.getLength());

					Log.i("info", "receive from " + IPAddress + " msg:" + checkcodeRes);

					if(Integer.parseInt(PackageBuilder.getLinkACK(checkcodeRes)) == Integer.parseInt(checkCode) + 1){
						Log.i("info", "ACK received");
						checked = true;
					}else{
						Log.i("info", "No ACK response");
						checked = false;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}catch (NullPointerException e){
					e.printStackTrace();
				}finally {
					datagramSocketCheck.close();
					datagramSocket.close();
				}
			}
		});
		thread.start();
		try {
			Thread.sleep(TIME_OUT * 2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return checked;
	}

	/**
	 * 广播发送一个消息,连接时使用,不能在主线程中使用
	 * @param msg 消息体
	 * @param checkCode 响应码
	 * @param context 上下文
	 * @return 如果得到响应就返回true并将IP地址设定为响应地址，否则返回false
	 */
	public static boolean sendWithCheckBroadcast(final String msg, final String checkCode, final Context context) throws IOException{

		boolean connected = true;
		final InetAddress IPAddress = getBroadcastAddress(context);

		DBHelper dbHelper = new DBHelper();
		dbHelper.init(context);
		dbHelper.open();
		final int port = dbHelper.getPort();
		dbHelper.close();


		if(!connected){
			Log.i("info", "disconnected!");
			return false;
		}

		checked = false;

		try {
			datagramSocket = new DatagramSocket();
			datagramPacket = new DatagramPacket(msg.getBytes(), msg.getBytes().length, IPAddress, port);
			datagramSocket.send(datagramPacket);
			Log.i("info", "broadcast " + IPAddress + ":" + port + " msg:" + msg);

			byte[] check = new byte[ACK_SIZE];
			int checkport = port + 1;
			datagramSocketCheck = new DatagramSocket(checkport);
			datagramPacketCheck = new DatagramPacket(check, check.length);

			//等待时间TIME_OUT秒
			datagramSocketCheck.setSoTimeout(TIME_OUT);
			datagramSocketCheck.receive(datagramPacketCheck);
			String checkcodeRes = new String(datagramPacketCheck.getData(), 0, datagramPacketCheck.getLength());

			Log.i("info", "receive  " + datagramPacketCheck.getAddress().getHostAddress() + ":" + datagramPacketCheck.getPort() + " msg:" + checkcodeRes);

			Log.i("info", "ACK: " + PackageBuilder.getLinkACK(checkcodeRes));
			Log.i("info", "checkCode: " + checkCode);

			String responseACK = PackageBuilder.getLinkACK(checkcodeRes);
			if(responseACK.equals("NULL")){
				checked = false;
				return checked;
			}

			if(Integer.parseInt(PackageBuilder.getLinkACK(checkcodeRes)) == Integer.parseInt(checkCode)){
				Log.i("info", "ACK received");
				String ip = datagramPacketCheck.getAddress().getHostAddress();
				UDPSender.setAddr(ip, context);
				dbHelper = new DBHelper();
				dbHelper.init(context);
				dbHelper.open();
				dbHelper.setPCName(PackageBuilder.getLinkMSG(checkcodeRes));
				dbHelper.close();
				checked = true;
			}else{
				Log.i("info", "ACK response was wrong!");
				checked = false;
			}

		}catch (SocketTimeoutException e) {
			Log.i("info", "Time out! No ACK response!");
		}catch (NullPointerException e){
			e.printStackTrace();
		}catch (IOException e){
			e.printStackTrace();
		}finally {
			datagramSocket.close();
			datagramSocketCheck.close();
		}
		return checked;
	}

	/**
	 * 广播发送一个消息
	 * @param msg 消息体
	 * @param checkCode 响应码
	 * @param context 上下文
	 * @handler 执行动画效果的handler对象
	 * @return 如果得到响应就返回true并将IP地址设定为响应地址，否则返回false，如果处于未连接状态则不发送直接返回false
	 */
	public static boolean sendWithCheckBroadcast(final String msg, final String checkCode, final Context context, Handler handler) throws IOException{


		boolean connected = true;
		final InetAddress IPAddress = getBroadcastAddress(context);

		DBHelper dbHelper = new DBHelper();
		dbHelper.init(context);
		dbHelper.open();
		final int port = dbHelper.getPort();
		dbHelper.close();


		if(!connected){
			Log.i("info", "disconnected!");
			return false;
		}

		checked = false;
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					datagramSocket = new DatagramSocket();
					datagramPacket = new DatagramPacket(msg.getBytes(), msg.getBytes().length, IPAddress, port);
					datagramSocket.send(datagramPacket);
					Log.i("info", "broadcast " + IPAddress + ":" + port + " msg:" + msg);

					byte[] check = new byte[ACK_SIZE];
					int checkport = port + 1;
					datagramSocketCheck = new DatagramSocket(checkport);
					datagramPacketCheck = new DatagramPacket(check, check.length);

					//等待时间TIME_OUT秒
					datagramSocketCheck.setSoTimeout(TIME_OUT);
					datagramSocketCheck.receive(datagramPacketCheck);
					String checkcodeRes = new String(datagramPacketCheck.getData(), 0, datagramPacketCheck.getLength());

					Log.i("info", "receive  " + datagramPacketCheck.getAddress().getHostAddress() + ":" + datagramPacketCheck.getPort() + " msg:" + checkcodeRes);

					Log.i("info", "Regex : " + PackageBuilder.getLinkACK(checkcodeRes));

					if(Integer.parseInt(PackageBuilder.getLinkACK(checkcodeRes)) == Integer.parseInt(checkCode)){
						Log.i("info", "ACK received");
						String ip = datagramPacketCheck.getAddress().getHostAddress();
						UDPSender.setAddr(ip, context);
						checked = true;
					}else{
						Log.i("info", "No ACK response");
						checked = false;
					}

				} catch (IOException e) {
					e.printStackTrace();
				}catch (NullPointerException e){
					e.printStackTrace();
				}finally {
					datagramSocket.close();
					datagramSocketCheck.close();
				}
			}
		});
		thread.start();
		try {
			int time = 0;	//已等待时间
			while(!checked){
				Thread.sleep(TIME_OUT / 10);
				time += TIME_OUT / 10;
				if(time >= TIME_OUT * 2){
					break;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return checked;
	}

	/**
	 * 返回当前网络环境下的广播地址
	 * @param context 上下文
	 * @return 地址的InetAddress对象
	 * @throws IOException
	 */
	private static InetAddress getBroadcastAddress(Context context) throws IOException{
		WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
		if(dhcpInfo == null){
			Log.i("info", "can't get broadcast address!");
			return null;
		}

		byte[] quads = new byte[4];
		for(int k = 0; k < 4; k++){

			int g = (dhcpInfo.gateway >> (k * 8)) & 0xFF;
			int m = (dhcpInfo.netmask >> (k * 8)) & 0xFF;

			quads[k] = (byte)(g & m |~ m);
		}
		return InetAddress.getByAddress(quads);
	}

	/**
	 * 向指定地址发送一个消息，如果处于未连接状态则不发送
	 * @param msg 消息体
	 * @param context 上下文
	 */
	public static void send(final String msg, Context context){

		DBHelper dbHelper = new DBHelper();
		dbHelper.init(context);
		dbHelper.open();
		boolean connected = dbHelper.isConnected();
		final InetAddress IPAddress = dbHelper.getIPAddr();
		final int port = dbHelper.getPort();
		dbHelper.close();


		if(!connected){
			Log.i("info", "disconnected!");
			return;
		}

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					datagramSocket = new DatagramSocket();
					datagramPacket = new DatagramPacket(msg.getBytes(), msg.getBytes().length, IPAddress, port);
					datagramSocket.send(datagramPacket);
					Log.i("info", "send to " + IPAddress + ":" + port + " msg:" + msg);
				} catch (IOException e) {
					e.printStackTrace();
				}catch (NullPointerException e){
					e.printStackTrace();
					System.out.println("未设置地址或者未设置UDP包！");
				}
				finally {
					datagramSocket.close();
				}
			}
		});
		thread.start();
	}

	/**
	 * 设置当前连接状态，并将设置写入数据库
	 * @param status 是否处于连接状态
	 * @param context 上下文
	 */
	public static void setStatus(boolean status, Context context){
		DBHelper dbHelper = new DBHelper();
		dbHelper.init(context);
		dbHelper.open();
		dbHelper.setConnected(status);
		dbHelper.close();
	}

}
