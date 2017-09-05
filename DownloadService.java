package com.gogowan.petrochina.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;

import com.gogowan.petrochina.R;
import com.gogowan.petrochina.base.PalUtils;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadService extends Service {

	/********download progress step*********/
	private static final int down_step_custom = 1;
	// 通知栏消息
	private int messageNotificationID = 19930903;
	private static final int TIMEOUT = 10 * 1000;// 超时
	private static String down_url;
	private static final int DOWN_OK = 1;
	private static final int DOWN_ERROR = 0;
	//app的名字
	private String app_name;
	private NotificationManager notificationManager;
//	private Notification notification;
	private NotificationCompat.Builder notifyBuilder;
	private PendingIntent pendingIntent;
//	private RemoteViews contentView;


	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	/**
	 * 方法描述：onStartCommand方法
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notifyBuilder = new NotificationCompat.Builder(this);
		if(null == intent){
			stopSelf();
		}else {
			app_name = intent.getStringExtra("Key_App_Name");
			down_url = intent.getStringExtra("Key_Down_Url");
//			down_url = "http://a.wdjcdn.com/release/files/phoenix/5.16.0.12022/wandoujia-wandoujia-web_direct_binded_5.16.0.12022.apk?remove=2&append=%8C%00eyJhcHBEb3dubG9hZCI6eyJkb3dubG9hZFR5cGUiOiJkb3dubG9hZF9ieV9wYWNrYWdlX25hbWUiLCJwYWNrYWdlTmFtZSI6ImNvbS5kdW9rYW4ucmVhZGVyIn19Wdj01B00007c5522";
			// create file,应该在这个地方加一个返回值的判断SD卡是否准备好，文件是否创建成功，等等！
			PalUtils.createFile(app_name);
			if(PalUtils.isCreateFileSucess == true){
				createNotification();
				createThread();
			}else{
				stopSelf();
			}
		}
		//被杀死之后，在内存充足的情况下，系统在有充足的情况下会尝试重新创建该服务
		return Service.START_STICKY;
	}

	/**
	 * update UI
	 * */
	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case DOWN_OK:
					/*********1下载完成，点击安装***********/
					Uri uri = Uri.fromFile(PalUtils.updateFile);
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(uri,"application/vnd.android.package-archive");
					pendingIntent = PendingIntent.getActivity(DownloadService.this, 0, intent, 0);
					notifyBuilder.setContentText("下载成功 点击安装");
					notifyBuilder.setAutoCancel(true);
					notifyBuilder.setOngoing(false);
					notifyBuilder.setProgress(0, 0, false);
					notifyBuilder.setContentIntent(pendingIntent);
					notificationManager.notify(messageNotificationID,notifyBuilder.build());
					installApk();
					/***stop service*****/
					stopSelf();
					break;

				case DOWN_ERROR:
					notifyBuilder.setContentText("下载失败");
					notifyBuilder.setAutoCancel(true);
					notifyBuilder.setOngoing(false);
					notifyBuilder.setProgress(0, 0, false);
					notificationManager.notify(messageNotificationID,notifyBuilder.build());
					stopSelf();
					break;

				default:
					break;
			}
		}
	};

	/**
	 * installApk
	 * */
	private void installApk() {
		/*********下载完成，点击安装***********/
		Uri uri = Uri.fromFile(PalUtils.updateFile);
		Intent intent = new Intent(Intent.ACTION_VIEW);
		/**********加这个属性是因为使用Context的startActivity方法的话，就需要开启一个新的task**********/
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setDataAndType(uri,"application/vnd.android.package-archive");
		DownloadService.this.startActivity(intent);
	}

	/**
	 * 开线程下载
	 */
	public void createThread() {
		new DownLoadThread().start();
	}

	/**
	 * 下载线程
	 */
	private class DownLoadThread extends Thread{
		@Override
		public void run() {
			if(handler == null){
				return;
			}
			Message message = new Message();
			try {
				long downloadSize = downloadUpdateFile(down_url, PalUtils.updateFile.toString());
				if (downloadSize > 0) {
					// down success
					message.what = DOWN_OK;
					handler.sendMessage(message);
				}
			} catch (Exception e) {
				e.printStackTrace();
				message.what = DOWN_ERROR;
				handler.sendMessage(message);
			}
		}
	}

	/**
	 * createNotification
	 */
	public void createNotification() {
		PendingIntent pendingIntent = PendingIntent.getActivity(DownloadService.this, 0, new Intent(), 0);
		notifyBuilder.setSmallIcon(R.drawable.logo1);
		notifyBuilder.setContentTitle(app_name + " 正在下载");
		notifyBuilder.setContentText("当前下载进度： " + "0%");
		notifyBuilder.setContentIntent(pendingIntent);

		notifyBuilder.setTicker(app_name + " 正在下载");
		notifyBuilder.setOngoing(true);
		notifyBuilder.setProgress(100, 0, false);
		notificationManager.notify(messageNotificationID, notifyBuilder.build());
	}

	/***
	 * downloadUpdateFile
	 * @throws MalformedURLException
	 */
	public long downloadUpdateFile(String down_url, String file)throws Exception {

		int down_step = down_step_custom;// 提示step
		int totalSize;// 文件总大小
		int downloadCount = 0;// 已经下载好的大小
		int updateCount = 0;// 已经上传的文件大小

		InputStream inputStream;
		OutputStream outputStream;

		URL url = new URL(down_url);
		HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
		httpURLConnection.setConnectTimeout(TIMEOUT);
		httpURLConnection.setReadTimeout(TIMEOUT);
		// 获取下载文件的size
		totalSize = httpURLConnection.getContentLength();
		if (httpURLConnection.getResponseCode() == 404) {
			throw new Exception("fail!");
			//这个地方应该加一个下载失败的处理，但是，因为我们在外面加了一个try---catch，已经处理了Exception,
			//所以不用处理
		}
		inputStream = httpURLConnection.getInputStream();
		outputStream = new FileOutputStream(file, false);// 设置为false,文件存在则覆盖掉
		byte buffer[] = new byte[1024];
		int readsize = 0;
		while ((readsize = inputStream.read(buffer)) != -1) {
			outputStream.write(buffer, 0, readsize);
			downloadCount += readsize;// 时时获取下载到的大小
			if (updateCount == 0 || (downloadCount * 100 / totalSize - down_step) >= updateCount) {
				updateCount += down_step;
				notifyBuilder.setProgress(100, updateCount, false);
				notifyBuilder.setContentText("当前下载进度： " + updateCount + "%");
				notificationManager.notify(messageNotificationID, notifyBuilder.build());
			}
		}
		if (httpURLConnection != null) {
			httpURLConnection.disconnect();
		}
		inputStream.close();
		outputStream.close();
		return downloadCount;
	}
}