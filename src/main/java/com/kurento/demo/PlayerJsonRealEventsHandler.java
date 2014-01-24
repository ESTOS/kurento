package com.kurento.demo;

import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerService;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.ZBarFilter;
import com.kurento.kmf.media.events.CodeFoundEvent;
import com.kurento.kmf.media.events.MediaEventListener;

/**
 * HTTP Player Handler which plays a media pipeline composed by a
 * <code>PlayerEndPoint</code> with a <code>ZBarFilter</code>; using redirect
 * strategy; with JSON signalling protocol.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 */
@HttpPlayerService(name = "PlayerJsonRealEventsHandler", path = "/playerJsonEvents", redirect = true, useControlProtocol = true)
public class PlayerJsonRealEventsHandler extends HttpPlayerHandler {

	private String url = "";

	@Override
	public void onContentRequest(final HttpPlayerSession session)
			throws Exception {
		MediaPipelineFactory mpf = session.getMediaPipelineFactory();
		MediaPipeline mp = mpf.create();
		session.releaseOnTerminate(mp);

		PlayerEndpoint playerEndpoint = mp.newPlayerEndpoint(
				VideoURLs.map.get("zbar")).build();
		ZBarFilter filter = mp.newZBarFilter().build();
		playerEndpoint.connect(filter);

		filter.addCodeFoundDataListener(new MediaEventListener<CodeFoundEvent>() {

			@Override
			public void onEvent(CodeFoundEvent event) {
				getLogger().info(
						"Event " + event.getType() + "-->" + event.getValue());
				if (url.equals(event.getValue())) {
					return;
				} else {
					url = event.getValue();
					session.publishEvent(new ContentEvent(event.getType(),
							event.getValue()));
				}
			}
		});
		session.setAttribute("player", playerEndpoint);
		session.start(filter);
	}

	@Override
	public void onContentStarted(HttpPlayerSession session) {
		PlayerEndpoint playerEndpoint = (PlayerEndpoint) session
				.getAttribute("player");
		playerEndpoint.play();
	}

}
