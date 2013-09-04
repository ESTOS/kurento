package com.kurento.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;

@PlayerService(name = "PlayerConsumingRtpJackVaderFilter", path = "/playerRtpJack", useControlProtocol = true)
public class PlayerConsumingRtpJackVaderFilter implements PlayerHandler {
	private static final Logger log = LoggerFactory
			.getLogger(PlayerConsumingRtpJackVaderFilter.class);

	@Override
	public void onPlayRequest(PlayRequest playRequest) throws ContentException {
		log.info("Received request to " + playRequest);
		try {
			if (RtpProducingJackVaderFilter.sharedJackVaderReference == null) {
				log.info("Found sharedJackVaderReference ... invoking play");
				playRequest
						.play(RtpProducingJackVaderFilter.sharedJackVaderReference);
			} else {
				log.info("Cannot find sharedJackVaderReference instance ... rejecting request");
				playRequest
						.reject(500,
								"Cannot find sharedJackVaderReference instance ... rejecting request");
			}
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			playRequest.reject(500, t.getMessage());
		}

	}

	@Override
	public void onContentPlayed(PlayRequest playRequest) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onContentError(PlayRequest playRequest,
			ContentException exception) {
		// TODO Auto-generated method stub

	}

}
