package http2.client;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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



public class Http2ClientBenchMark {
	//Just testing egit

	public static final String KEYSTORE = new File("JGet.jks").getAbsolutePath();

	public static final String STOREPASS = "welcome1";

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

		for(int i=0;i<urlsListSize;i++)
		{
			//Request Object
			//MetaData.Request metaData = new MetaData.Request("GET", new HttpURI("https://" + host + ":" + port + "/"), HttpVersion.HTTP_2, requestFields);
			//MetaData.Request metaData = new MetaData.Request("GET", new HttpURI("https://webtide.com:443/"), HttpVersion.HTTP_2, requestFields);
			System.out.println(new HttpURI("https://" + host + ":" + port + urlsList.get(i).substring(urlsList.get(i).indexOf('/',8))));
			MetaData.Request metaDataRequest = new MetaData.Request("GET", new HttpURI("https://" + host + ":" + port + urlsList.get(i).substring(urlsList.get(i).indexOf('/',8))), HttpVersion.HTTP_2, requestFields);
			//Create Headers frame
			HeadersFrame headersFrame = new HeadersFrame(metaDataRequest,null,false);

			//Listen to response frames.
			System.out.println("Going to start streams");
			session.newStream(headersFrame, new Promise.Adapter<Stream>(), new Stream.Listener.Adapter()
			{
				@Override
				public void onHeaders(Stream stream, HeadersFrame frame)
				{
					//System.err.println(frame);
					//System.out.println("[" + frame.getStreamId() + "] HEADERS " + frame.getMetaData().toString());
					//frame.getMetaData().getFields().forEach(field -> System.out.println("[" + stream.getId() + "]     " + field.getName() + ": " + field.getValue()));
					if (frame.isEndStream()) {
						System.out.println("on Headers frame end" + frame.getStreamId());
						//phaser.arrive();
					}
				}

				@Override
				public void onData(Stream stream, DataFrame frame, Callback callback)
				{
					//System.err.println(frame);
					//byte[] bytes = new byte[frame.getData().remaining()];
					//frame.getData().get(bytes);
					//System.out.println("[" + frame.getStreamId() + "] DATA " + new String(bytes));
					//callback.succeeded();
					//System.out.println(frame.toString());
					if (frame.isEndStream()) {
						System.out.println("on data frame end" + frame.getStreamId());
						//phaser.arrive();
					}
				}

				@Override
				public Stream.Listener onPush(Stream stream, PushPromiseFrame frame)
				{
					//System.err.println(frame);
					//System.out.println("[" + frame.getStreamId() + "] PUSH_PROMISE " + frame.getMetaData().toString());
					System.out.println("Push promise frame received " + frame.getPromisedStreamId() + frame.getStreamId());
					//phaser.register();
					return this;
				}
			});

		}


		Thread.sleep(20000);

		lowLevelClient.stop();
	}

	   private static void enableSSLDebug()
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
}
