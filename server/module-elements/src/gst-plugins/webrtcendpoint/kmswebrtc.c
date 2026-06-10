/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#include "kmswebrtcendpoint.h"
#include "config.h"

#define PLUGIN_NAME "webrtcendpoint"

gboolean
kms_webrtc_endpoint_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_WEBRTC_ENDPOINT);
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    webrtcendpoint,
    "Kurento WebRTC endpoint",
    kms_webrtc_endpoint_plugin_init, VERSION, GST_LICENSE_UNKNOWN,
    "Kurento Elements", "https://kurento.openvidu.io/")
