package http2.client;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.server.ServerSessionListener;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.util.FuturePromise;
import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.http2.frames.PushPromiseFrame;
import org.eclipse.jetty.http2.api.Stream;

/**
 * 
 * @author intel
 * How to run : java -Xbootclasspath/p:alpn-boot-8.1.7.v20160121.jar -jar http_2_asynchronousclient.jar all_urls.txt &> output.txt
 */

public class Http2ClientBenchMark {
	//Just testing egit

	//public static final String KEYSTORE = new File("JGet.jks").getAbsolutePath();

	//public static final String STOREPASS = "welcome1";


	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		//Read urls file and store all urls in a list
		String fileName = args[0];
		List<String> urlsList = new ArrayList<String>();
		int urlsListSize=0;

		urlsList = Files.readAllLines(Paths.get(fileName));
		urlsListSize = urlsList.size();

		/*try (Stream<String> stream = Files.lines(Paths.get(fileName))) {

			urlsList = stream.collect(Collectors.toList());
			urlsListSize = urlsList.size();

		} catch (IOException e) {
			System.out.println("Cannot open file");
			e.printStackTrace();
		}*/

		System.out.println(urlsListSize);
		HTTP2Client lowLevelClient = new HTTP2Client();
		SslContextFactory sslContextFactory = new SslContextFactory(true);
		//SslContextFactory sslContextFactory = getSSLContextFactory ();
		lowLevelClient.addBean(sslContextFactory);
		lowLevelClient.start();
		System.out.println("Client Started");

		//String host =  "webtide.com";
		String host =  urlsList.get(0).substring(8, urlsList.get(0).indexOf(':', 8));
		//int port =  443;
		int port =  Integer.parseInt(urlsList.get(0).substring(urlsList.get(0).indexOf(':', 8)+1, urlsList.get(0).indexOf('/', 8)));

		final AtomicInteger success_status_count = new AtomicInteger(0);
		System.out.println("Host is " + host);
		System.out.println("Port is" + port);
		// Connect to host
		FuturePromise<Session> sessionPromise = new FuturePromise<>();
		lowLevelClient.connect(sslContextFactory, new InetSocketAddress(host, port), new ServerSessionListener.Adapter(), sessionPromise);
		//Obtain Client Session
		Session session = sessionPromise.get();

		System.out.println(session.getStreams().size());
		//Prepare the request headers

		//  	:authority:localhost:8443
		//		:method:GET
		//		:path:/my-gallery/thumbs/sr.jpg
		//		:scheme:https
		//		accept:image/webp,image/*,*/*;q=0.8
		//	accept-encoding:gzip, deflate, sdch
		//	accept-language:en-GB,en-US;q=0.8,en;q=0.6
		//	cache-control:no-cache
		//	pragma:no-cache
		//	referer:https://localhost:8443/my-gallery/index.html
		//		user-agent:Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/48.0.2564.82 Chrome/48.0.2564.82 Safari/537.36

		HttpFields requestFields = new HttpFields();
		requestFields.put(HttpHeader.USER_AGENT, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/48.0.2564.82 Chrome/48.0.2564.82 Safari/537.36");
		
		// Hardcoded to 100 to respect h2o settings frame. Can be easily generalized as well by reading onSetting frame in a class overriding Session.listener
		Semaphore no_of_concurrent_streams = new Semaphore(100);
		//final CountDownLatch latch = new CountDownLatch(it should be  = urlsListSize +  no_of_pushed_resources);
		//final CountDownLatch latch = new CountDownLatch(220);
		final Phaser phaser = new Phaser();
		long start =  System.currentTimeMillis();
		phaser.register();
		for(int i=0;i<urlsListSize;i++)
		{

			no_of_concurrent_streams.acquire();
			//requestFields.put(HttpHeader.C_AUTHORITY,host+":"+port);
			//requestFields.put(HttpHeader.C_METHOD, "GET");
			//requestFields.put(HttpHeader.C_SCHEME, "https");
			requestFields.put(HttpHeader.ACCEPT_ENCODING, "gzip, deflate, sdch");
			requestFields.put(HttpHeader.ACCEPT_LANGUAGE, "en-GB,en-US;q=0.8,en;q=0.6");
			requestFields.put(HttpHeader.CACHE_CONTROL, "no-cache");
			requestFields.put(HttpHeader.PRAGMA, "no-cache");
			String extension = urlsList.get(i).substring(urlsList.get(i).indexOf('.')+1);
			if(extension.equalsIgnoreCase("html"))
			{
				requestFields.put(HttpHeader.ACCEPT,"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
				requestFields.put(HttpHeader.C_PATH, urlsList.get(i).substring(urlsList.get(i).indexOf('/',8)));
			}
			else if(extension.equalsIgnoreCase("js"))
			{
				
				requestFields.put(HttpHeader.ACCEPT, "*/*");
				
				requestFields.put(HttpHeader.C_PATH, urlsList.get(i).substring(urlsList.get(i).indexOf('/',8)));
				requestFields.put(HttpHeader.REFERER, "https://" + host + ":" + port + urlsList.get(i).substring(urlsList.get(i).indexOf('/',8)));
			}
			else if(extension.equalsIgnoreCase("css"))
			{
				
				requestFields.put(HttpHeader.ACCEPT, "text/css, */*;q=0.1");
				
				requestFields.put(HttpHeader.C_PATH, urlsList.get(i).substring(urlsList.get(i).indexOf('/',8)));
				requestFields.put(HttpHeader.REFERER, "https://" + host + ":" + port + "/my-gallery/index.html");
			}
			else if(extension.equalsIgnoreCase("JSON"))
			{
				
				requestFields.put(HttpHeader.ACCEPT, "application/json");
				
				requestFields.put(HttpHeader.C_PATH, urlsList.get(i).substring(urlsList.get(i).indexOf('/',8)));
				requestFields.put(HttpHeader.REFERER, "https://" + host + ":" + port + "/my-gallery/index.html");
			}
			else if(extension.equalsIgnoreCase("gif")||extension.equalsIgnoreCase("jpg")||extension.equalsIgnoreCase("png"))
			{
				
				requestFields.put(HttpHeader.ACCEPT, "image/webp,image/*,*/*;q=0.8");

				requestFields.put(HttpHeader.C_PATH, urlsList.get(i).substring(urlsList.get(i).indexOf('/',8)));
				requestFields.put(HttpHeader.REFERER, "https://" + host + ":" + port + "/my-gallery/index.html");
			} 
			//Request Object
			//MetaData.Request metaData = new MetaData.Request("GET", new HttpURI("https://" + host + ":" + port + "/"), HttpVersion.HTTP_2, requestFields);
			//MetaData.Request metaData = new MetaData.Request("GET", new HttpURI("https://webtide.com:443/"), HttpVersion.HTTP_2, requestFields);
			//System.out.println(new HttpURI("https://" + host + ":" + port + urlsList.get(i).substring(urlsList.get(i).indexOf('/',8))));
			//String resource = urlsList.get(i);
			
			MetaData.Request metaDataRequest = new MetaData.Request("GET", new HttpURI("https://" + host + ":" + port + urlsList.get(i).substring(urlsList.get(i).indexOf('/',8))), HttpVersion.HTTP_2, requestFields);
			//Create Headers frame
			HeadersFrame headersFrame = new HeadersFrame(metaDataRequest,null,true);

			//Listen to response frames.
			//System.out.println("Going to start streams");
			phaser.register();
			session.newStream(headersFrame, new Promise.Adapter<Stream>(), new Stream.Listener.Adapter()
			{
			
				@Override
				public void onHeaders(Stream stream, HeadersFrame frame)
				{
					System.out.println("Header frame " + frame);
					//System.out.println("[" + frame.getStreamId() + "] HEADERS " + frame.getMetaData().toString());
					//frame.getMetaData().getFields().forEach(field -> System.out.println("[" + stream.getId() + "]     " + field.getName() + ": " + field.getValue()));
					if (frame.isEndStream()) {
						System.out.println("on Headers frame end" + frame.getStreamId());
						phaser.arrive();
						//latch.countDown();
						no_of_concurrent_streams.release();
					}
				}

				@Override
				public void onData(Stream stream, DataFrame frame, Callback callback)
				{
					System.out.println("Data frame" + frame);
					//byte[] bytes = new byte[frame.getData().remaining()];
					//frame.getData().get(bytes);
					//System.out.println("[" + frame.getStreamId() + "] DATA " + new String(bytes));
					//callback.succeeded();
					//System.out.println(frame.toString());
					callback.succeeded();
					if (frame.isEndStream()) {
						System.out.println("on data frame end" + frame.getStreamId());
						phaser.arrive();
						//latch.countDown();
						no_of_concurrent_streams.release();
					}
				}

				@Override
				public Stream.Listener onPush(Stream stream, PushPromiseFrame frame)
				{
					//System.err.println(frame);
					//System.out.println("[" + frame.getStreamId() + "] PUSH_PROMISE " + frame.getMetaData().toString());
					try {
						no_of_concurrent_streams.acquire();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println("Push promise frame received with promised stream id " + frame.getPromisedStreamId() + " Current stream id " +  frame.getStreamId());
					phaser.register();
					return this;
				}
			});
		}

		phaser.arriveAndAwaitAdvance();
		System.out.println(System.currentTimeMillis() - start + "ms" );
        //latch.await();
		//Thread.sleep(20000);

		lowLevelClient.stop();
	}

}


/*	private static void enableSSLDebug()
		{
			System.setProperty("javax.net.debug", "ssl");
		}

		private static SslContextFactory getSSLContextFactory() {
			// TODO Auto-generated method stub
			SslContextFactory sslContextFactory = new SslContextFactory();
			try
			{
				sslContextFactory.setNeedClientAuth(false);

				sslContextFactory.setKeyStorePath(new File(KEYSTORE).getAbsolutePath());
				sslContextFactory.setKeyStorePassword (STOREPASS);
				sslContextFactory.setKeyStoreType("JKS");

				sslContextFactory.setTrustStorePath(new File(KEYSTORE).getAbsolutePath());
				sslContextFactory.setTrustStorePassword(STOREPASS);
				sslContextFactory.setTrustStoreType("JKS");

				sslContextFactory.setKeyManagerPassword(STOREPASS);

			}
			catch (Exception e)
		{
			e.printStackTrace();
		}      
		return sslContextFactory;
	}
}*/

