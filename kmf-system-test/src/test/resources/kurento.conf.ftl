[Server]
serverAddress=${serverAddress}
serverPort=${serverPort}
sdpPattern=pattern.sdp

[HttpEPServer]
#serverAddress=localhost

# Announced IP Address may be helpful under situations such as the server needs
# to provide URLs to clients whose host name is different from the one the
# server is listening in. If this option is not provided, http server will try
# to look for any available address in your system.
# announcedAddress=localhost

serverPort=${httpEndpointPort}

[WebRtcEndPoint]
#stunServerAddress = xxx.xxx.xxx.xxx
#stunServerPort = xx
#pemCertificate = file