import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.log4j.BasicConfigurator;
import twitter4j.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CellularLive {

    public static void main(String[] args) {

        BasicConfigurator.configure();

        // Set up blocking queues
        final BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);
        BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>(1000);

        // Declare host connection, the endpoint, and authentication (basic auth or oauth)
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

        // Track some terms
        final List<String> terms = Lists.newArrayList("CAdnauseum: "); // Random words to listen to
        hosebirdEndpoint.trackTerms(terms);

        // Read secrets from properties files
        Properties prop = new Properties();

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("twitter4j.properties");

        try {
            prop.load(is);

            Authentication hosebirdAuth = new OAuth1(
                    prop.getProperty("oauth.consumerKey"),
                    prop.getProperty("oauth.consumerSecret"),
                    prop.getProperty("oauth.accessToken"),
                    prop.getProperty("oauth.accessTokenSecret"));

            // Create client
            ClientBuilder builder = new ClientBuilder()
                    .name("Hosebird-Client-01")
                    .hosts(hosebirdHosts)
                    .authentication(hosebirdAuth)
                    .endpoint(hosebirdEndpoint)
                    .processor(new StringDelimitedProcessor(msgQueue))
                    .eventMessageQueue(eventQueue);

            final Client hosebirdClient = builder.build();

            // Attempt connection
            hosebirdClient.connect();

            // Listen to messages
            Thread thread = new Thread(){
                public void run(){
                    System.out.println("Thread Running");

                    while (!hosebirdClient.isDone()) {
                        String msg = null;
                        try {
                            msg = msgQueue.take();
                            System.out.println("Message: " + msg);

                            try {
                                JSONObject jsonObj = new JSONObject(msg);

                                String inReplyToTweetId = jsonObj.get("id").toString(); // Get the tweet status id

                                String lastGen = jsonObj.get("text").toString();

                                // CAdnauseum: -xxxx----x-x-x-x-x--x

                                String lastGenClean = lastGen.replace("CAdnauseum: ", "");
                                lastGenClean = lastGenClean.split("\n")[0];

                                String start= lastGenClean;

                                int numGens = 10;
                                StringBuilder output = new StringBuilder();
                                String lastOutput = null;

                                for(int i= 0; i < numGens; i++){
                                    if (i == 9) {
                                        lastOutput = life(start);
                                    } else {
                                        output.append(start + "\n");
                                        start = life(start);
                                    }
                                }

                                String finalOutput = "CAdnauseum: " + lastOutput + "\n\n" + output;

                                replyTo(inReplyToTweetId, String.valueOf(finalOutput));

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        try {
                            Thread.sleep(39999); // Wait 39.999 seconds
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }

                }
            };
            thread.start();

        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }


    public static String life(String lastGen){
        String newGen= "";
        for(int i= 0; i < lastGen.length(); i++){
            int neighbors= 0;
            if (i == 0){//left edge
                neighbors= lastGen.charAt(1) == '-' ? 1 : 0;
            } else if (i == lastGen.length() - 1){//right edge
                neighbors= lastGen.charAt(i - 1) == 'x' ? 1 : 0;
            } else{//middle
                neighbors= getNeighbors(lastGen.substring(i - 1, i + 2));
            }

            if (neighbors == 0){//dies or stays dead with no neighbors
                newGen+= "x";
            }
            if (neighbors == 1){//stays with one neighbor
                newGen+= lastGen.charAt(i);
            }
            if (neighbors == 2){//flips with two neighbors
                newGen+= lastGen.charAt(i) == 'x' ? "-" : "x";
            }
        }
        return newGen;
    }


    public static int getNeighbors(String group){
        int ans= 0;
        if (group.charAt(0) == 'x') ans++;
        if (group.charAt(2) == 'x') ans++;
        return ans;
    }


    public static void replyTo(String inReplyToTweetId, String newGen) {
        Twitter twitter = new TwitterFactory().getInstance();

        Status reply = null;

        try {
            reply = twitter.updateStatus(new StatusUpdate(newGen).inReplyToStatusId(Long.parseLong(inReplyToTweetId)));

            System.out.println("Posted reply " + reply.getId() + " in response to tweet " + reply.getInReplyToStatusId());
        } catch (TwitterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


}