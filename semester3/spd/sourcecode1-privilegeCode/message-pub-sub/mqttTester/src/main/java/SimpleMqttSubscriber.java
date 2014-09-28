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

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class SimpleMqttSubscriber implements MqttCallback {

    MqttClient myClient;
    MqttConnectOptions connOpt;

    static String url = "tcp://localhost:1883";
    static Boolean subscriber = true;
    static String password = "admin";
    static String username = "admin";
    static String topic = "temperatureStream";

    @Override
    public void connectionLost(Throwable t) {
        System.out.println("Connection lost!");
        // code to reconnect to the broker would go here if desired
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        System.out.println("-------------------------------------------------");
        System.out.println("| Topic:" + s);
        System.out.println("| Message: " + new String(mqttMessage.getPayload()));
        System.out.println("-------------------------------------------------");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    public static void main(String[] args) {

        if (args.length == 1) {
            topic = args[0];
        } else if (args.length == 2) {
            topic = args[0];
            url = args[1];
        } else if (args.length == 3) {
            topic = args[0];
            url = args[1];
            username = args[2];
        } else if (args.length == 4) {
            topic = args[0];
            url = args[1];
            username = args[2];
            password = args[3];
        }

        SimpleMqttSubscriber smc = new SimpleMqttSubscriber();
        smc.runClient();
    }

    public void runClient() {

        connOpt = new MqttConnectOptions();

        connOpt.setCleanSession(true);
        connOpt.setKeepAliveInterval(30);
        connOpt.setUserName(username);
        connOpt.setPassword(password.toCharArray());

        try {
            myClient = new MqttClient(url, "SIMPLE-MQTT-SUB1");
            myClient.setCallback(this);
            myClient.connect(connOpt);

        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        System.out.println("Connected to " + url);

        // subscribe to topic if subscriber
        if (subscriber) {
            try {
                int subQoS = 0;
                myClient.subscribe(topic, subQoS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            // wait to ensure subscribed messages are delivered
            if (subscriber) {
                Thread.sleep(5000000);
            }
            myClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}