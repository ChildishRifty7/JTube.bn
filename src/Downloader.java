import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Gauge;

import cc.nnproject.json.JSONObject;

public class Downloader implements CommandListener, Constants, Runnable {
	
	private String id;
	private String res;
	
	private Alert alert;
	private Displayable d;
	private Gauge indicator;
	
	private String file;
	private Thread t;

	public Downloader(String vid, String res, Displayable d, String downloadDir) {
		this.id = vid;
		this.res = res;
		this.d = d;
		this.file = "file:///" + downloadDir;
		if(!(file.endsWith("/") || file.endsWith("\\"))) {
			file += "/";
		}
	}
	
	public void run() {
		FileConnection fc = null;
		OutputStream out = null;
		HttpConnection hc = null;
		InputStream in = null;
		try {
			file = file + id + ".mp4";
			info(id + ".mp4");
			
			JSONObject o = App.getVideoInfo(id, res);
			String url = o.getString("url");
			// подождать
			Thread.sleep(500);
			info("Connecting", 0);
			fc = (FileConnection) Connector.open(file, Connector.READ_WRITE);
			
			if (fc.exists()) {
				try {
					fc.delete();
				} catch (IOException e) {
				}
			}
			fc.create();
			out = fc.openOutputStream();
			hc = (HttpConnection) Connector.open(url);
			hc.setRequestProperty("User-Agent", downloadUserAgent);

			int r;
			try {
				r = hc.getResponseCode();
			} catch (IOException e) {
				info("Waiting...");
				hc.close();
				Thread.sleep(2000);
				hc = (HttpConnection) Connector.open(url);
				hc.setRequestMethod("GET");
				hc.setRequestProperty("User-Agent", downloadUserAgent);
				r = hc.getResponseCode();
			}
			int redirectCount = 0;
			while (r == 301 || r == 302) {
				info("Redirected (" + redirectCount++ + ")");
				String redir = hc.getHeaderField("Location");
				if (redir.startsWith("/")) {
					String tmp = url.substring(url.indexOf("//") + 2);
					String host = url.substring(0, url.indexOf("//")) + "//" + tmp.substring(0, tmp.indexOf("/"));
					redir = host + redir;
				}
				hc.close();
				hc = (HttpConnection) Connector.open(redir);
				hc.setRequestMethod("GET");
				hc.setRequestProperty("User-Agent", downloadUserAgent);
				r = hc.getResponseCode();
			}
			in = hc.openInputStream();
			info("Connected");
			int percent = 0;
			byte[] buf = new byte[128 * 1024];
			int read = 0;
			int downloaded = 0;
			int l = (int) hc.getLength();
			int i = 0;
			while((read = in.read(buf)) != -1) {
				out.write(buf, 0, read);
				downloaded += read;
				percent = (int)(((double)downloaded / (double)l) * 100d);
				if(i++ % 4 == 0) {
					Thread.sleep(1);
					info("Downloading " + percent + "%", percent);
				}
			}
			done();
		} catch (InterruptedException e) {
			fail("Canceled", "");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString(), "Download failed");
		} finally {
			try {
				if(out != null) out.close();
			} catch (Exception e) {
			} 
			try {
				if(fc != null) fc.close();
			} catch (Exception e) {
			} 
			try {
				if(in != null) in.close();
			} catch (Exception e) {
			} 
			try {
				if(hc != null) hc.close();
			} catch (Exception e) {
			} 
		}
	}
	
	public void start() {
		alert = new Alert("", "Initializing", null, AlertType.INFO);
		alert.addCommand(cancelCmd);
		alert.setTimeout(Alert.FOREVER);
		App.display(alert);
		alert.setCommandListener(this);
		this.indicator = new Gauge(null, false, 100, 0); 
		alert.setIndicator(indicator);
		t = new Thread(this);
		t.start();
	}

	private void done() {
		hideIndicator();
		info("Done");
		alert.addCommand(dlOkCmd);
		alert.addCommand(dlOpenCmd);
	}

	private void hideIndicator() {
		alert.removeCommand(dlCancelCmd);
		alert.setIndicator(null);
	}

	private void fail(String s, String title) {
		hideIndicator();
		alert.setTitle(title);
		alert.setString(s);
		alert.addCommand(dlOkCmd);
	}

	private void info(String s, int percent) {
		info(s);
		indicator.setValue(percent);
	}

	private void info(String s) {
		alert.setString(s);
	}

	public void commandAction(Command c, Displayable _d) {
		if(c == dlOkCmd) {
			App.display(d);
		}
		if(c == dlCancelCmd) {
			t.interrupt();
		}
		if(c == dlOpenCmd) {
			try {
				App.platReq(file);
				App.display(d);
			} catch (ConnectionNotFoundException e) {
				e.printStackTrace();
				info(e.toString());
			}
		}
	}

}
