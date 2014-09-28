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

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.broker.ProducerBrokerExchange;
import org.apache.activemq.broker.region.Subscription;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.Message;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.log4j.Logger;
import org.wso2.carbon.um.ws.api.stub.RemoteUserStoreManagerServiceStub;

import java.io.File;

public class UserAuthenticationBroker extends BrokerFilter {

    Logger log = Logger.getLogger(UserAuthenticationBroker.class);

    //Necessary default properties required
    final String serverUrl;
    final String username;
    final String password;
    final String jksFileLocation;
    final String publisherKey = "publisher";
    final String subscriberKey = "subscriber";
    final String adminKey = "admin";

    //RemoteStoreManager Clients
    String remoteUserStoreManagerAuthCookie = "";
    ServiceClient remoteUserStoreManagerServiceClient;
    RemoteUserStoreManagerServiceStub remoteUserStoreManagerServiceStub;

    public UserAuthenticationBroker(Broker next, String serverUrl, String username,
                                    String password, String jksFileLocation) {
        super(next);
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;
        this.jksFileLocation = jksFileLocation;
        createAdminClients();
    }


    public void addConnection(ConnectionContext context, ConnectionInfo info)
            throws Exception {

        Options option = remoteUserStoreManagerServiceClient.getOptions();
        option.setProperty(HTTPConstants.COOKIE_STRING, remoteUserStoreManagerAuthCookie);
        HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
        auth.setUsername(username);
        auth.setPassword(password);
        auth.setPreemptiveAuthentication(true);
        option.setProperty(HTTPConstants.AUTHENTICATE, auth);
        option.setManageSession(true);

        log.info("Authenticating the user .............");
        log.info("******************************************************");
        log.info("* Connecting to Identity Provider in Admin Privilege *");
        log.info("******************************************************\n");

        boolean isValidUser = remoteUserStoreManagerServiceStub.authenticate(info.getUserName(), info.getPassword());

        log.info("------------------------------------------------------");
        log.info("----------- Back to Normal Privilege -----------------");
        log.info("------------------------------------------------------\n");

        if (isValidUser) {
            log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            log.info("+++++++++  Valid user connection : " + info.getUserName());
            log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");

            super.addConnection(context, info);
        } else {
            throw new SecurityException("Not a valid user connection");
        }
    }


    public Subscription addConsumer(ConnectionContext context, ConsumerInfo info) throws Exception {

        String[] roleNames;

        Options option = remoteUserStoreManagerServiceClient.getOptions();
        option.setProperty(HTTPConstants.COOKIE_STRING, remoteUserStoreManagerAuthCookie);
        HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
        auth.setUsername(username);
        auth.setPassword(password);
        auth.setPreemptiveAuthentication(true);
        option.setProperty(HTTPConstants.AUTHENTICATE, auth);
        option.setManageSession(true);

        log.info("Authorizing the user to consume messages..........");
        log.info("******************************************************");
        log.info("* Connecting to Identity Provider in Admin Privilege *");
        log.info("******************************************************\n");

        roleNames = remoteUserStoreManagerServiceStub.getRoleListOfUser(context.getUserName());

        log.info("------------------------------------------------------");
        log.info("----------- Back to Normal Privilege -----------------");
        log.info("------------------------------------------------------\n");

        for (String role : roleNames) {
            if (role.equalsIgnoreCase(subscriberKey) || role.equalsIgnoreCase(adminKey)) {
                log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                log.info("+++++  Valid user to consume messages" + context.getUserName());
                log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");

                return super.addConsumer(context, info);
            }
        }
        throw new SecurityException("Not a valid user to subscribe or invalid topic name");
    }

    public void send(ProducerBrokerExchange producerExchange, Message messageSend)
            throws Exception {

        String[] roleNames;
        String userName = producerExchange.getConnectionContext().getUserName();

        Options option = remoteUserStoreManagerServiceClient.getOptions();
        option.setProperty(HTTPConstants.COOKIE_STRING, remoteUserStoreManagerAuthCookie);
        HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
        auth.setUsername(username);
        auth.setPassword(password);
        auth.setPreemptiveAuthentication(true);
        option.setProperty(HTTPConstants.AUTHENTICATE, auth);
        option.setManageSession(true);

        log.info("Authorizing the user to publish messages..........");
        log.info("******************************************************");
        log.info("* Connecting to Identity Provider in Admin Privilege *");
        log.info("******************************************************\n");

        roleNames = remoteUserStoreManagerServiceStub.getRoleListOfUser(userName);

        log.info("------------------------------------------------------");
        log.info("----------- Back to Normal Privilege -----------------");
        log.info("------------------------------------------------------\n");

        for (String role : roleNames) {
            if (role.equalsIgnoreCase(publisherKey) || role.equalsIgnoreCase(adminKey)) {
                log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                log.info("+++++  Valid user to publish messages ++++++++++++");
                log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");

                super.send(producerExchange, messageSend);
                return;
            }
        }

        throw new SecurityException("Not a valid user to publish events");
    }


    private void createAdminClients() {

        /**
         * trust store path.  this must contains server's  certificate or Server's CA chain
         */

        String trustStore = jksFileLocation + File.separator + "wso2carbon.jks";

        /**
         * Call to https://localhost:9443/services/   uses HTTPS protocol.
         * Therefore we to validate the server certificate or CA chain. The server certificate is looked up in the
         * trust store.
         * Following code sets what trust-store to look for and its JKs password.
         */

        System.setProperty("javax.net.ssl.trustStore", trustStore);

        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

        /**
         * Axis2 configuration context
         */
        ConfigurationContext configContext;

        try {

            /**
             * Create a configuration context. A configuration context contains information for
             * axis2 environment. This is needed to create an axis2 service client
             */
            configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(null, null);

            /**
             * end point url with service name
             */
            String remoteUserStoreManagerServiceEndPoint = serverUrl + "/services/" + "RemoteUserStoreManagerService";

            /**
             * create stub and service client
             */
            remoteUserStoreManagerServiceStub = new RemoteUserStoreManagerServiceStub(configContext, remoteUserStoreManagerServiceEndPoint);
            remoteUserStoreManagerServiceClient = remoteUserStoreManagerServiceStub._getServiceClient();
            Options option = remoteUserStoreManagerServiceClient.getOptions();

            /**
             * Setting a authenticated cookie that is received from Carbon server.
             * If you have authenticated with Carbon server earlier, you can use that cookie, if
             * it has not been expired
             */
            option.setProperty(HTTPConstants.COOKIE_STRING, null);

            /**
             * Setting basic auth headers for authentication for carbon server
             */
            HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
            auth.setUsername(username);
            auth.setPassword(password);
            auth.setPreemptiveAuthentication(true);
            option.setProperty(HTTPConstants.AUTHENTICATE, auth);
            option.setManageSession(true);


            remoteUserStoreManagerAuthCookie = (String) remoteUserStoreManagerServiceStub._getServiceClient().getServiceContext()
                    .getProperty(HTTPConstants.COOKIE_STRING);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
