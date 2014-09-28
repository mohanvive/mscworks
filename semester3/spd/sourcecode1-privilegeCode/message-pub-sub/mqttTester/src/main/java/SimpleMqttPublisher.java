/*
* Copyright 2014 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class SimpleMqttPublisher {

    MqttClient myClient;
    MqttConnectOptions connOpt;

    static final String BROKER_URL = "tcp://localhost:1883";
    static final Boolean publisher = true;
    String myTopic = "temperatureStream";

    public static void main(String[] args) {
        SimpleMqttPublisher smc = new SimpleMqttPublisher();
        smc.runClient();
    }

    public void runClient() {

        connOpt = new MqttConnectOptions();

        connOpt.setCleanSession(true);
        connOpt.setKeepAliveInterval(30);
        connOpt.setUserName("admin");
        connOpt.setPassword("admin".toCharArray());

        try {
            myClient = new MqttClient(BROKER_URL, "SIMPLE-MQTT-PUB");
            myClient.connect(connOpt);

        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(-1);
        }


        System.out.println("Connected to " + BROKER_URL);

        if (publisher) {
            for (int i = 1; i <= 1; i++) {
                String pubMsg = "Temperature:29.0";
                int pubQoS = 1;
                MqttMessage message = new MqttMessage(pubMsg.getBytes());
                message.setQos(pubQoS);

                try {
                    myClient.publish(myTopic, message);
                } catch (MqttException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
        try {
            myClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}