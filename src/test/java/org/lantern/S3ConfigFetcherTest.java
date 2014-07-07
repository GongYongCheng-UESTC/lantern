package org.lantern;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import org.lantern.event.Events;
import org.lantern.event.MessageEvent;
import org.lantern.oauth.OauthUtils;
import org.lantern.oauth.RefreshToken;
import org.lantern.proxy.FallbackProxy;
import org.lantern.state.Mode;
import org.lantern.state.Model;
import org.lantern.util.DefaultHttpClientFactory;
import org.lantern.util.HttpClientFactory;

import com.google.common.eventbus.Subscribe;

public class S3ConfigFetcherTest {

    private final AtomicReference<MessageEvent> messageRef = 
            new AtomicReference<MessageEvent>();
    
    @Test
    public void testStopAndStart() throws Exception {
        final HttpClientFactory httpClientFactory = 
                TestingUtils.newHttClientFactory();
        final Model model = TestingUtils.newModel();
        model.getSettings().setMode(Mode.give);
        final OauthUtils oauth = new OauthUtils(httpClientFactory, model, new RefreshToken(model));
        final S3ConfigFetcher fetcher = new S3ConfigFetcher(model, oauth);
        
        model.setS3Config(null);
        fetcher.init();
        fetcher.start();
        assertNotNull(model.getS3Config());
        

        model.setS3Config(null);
        fetcher.stop();
        assertNull(model.getS3Config());
        
        fetcher.init();
        fetcher.start();
        assertNotNull(model.getS3Config());
    }
    
    @Test
    public void testWithExceptions() throws Exception {
        Events.register(this);
        final Model model = new Model();
        final OauthUtils oauth = mock(OauthUtils.class);
        when(oauth.getRequest(any(String.class))).thenThrow(new IOException());
        
        final S3ConfigFetcher fetcher = new S3ConfigFetcher(model, oauth);
        
        assertEquals(1, model.getS3Config().getFallbacks().size());
        
        model.getS3Config().setFallbacks(Arrays.asList(new FallbackProxy()));
        
        assertNull(messageRef.get());
        fetcher.init();
        
        assertEquals(2, model.getS3Config().getFallbacks().size());
        
        Thread.sleep(200);
        
        // We want to make sure the message is not sent here, as a single 
        // failure to download shouldn't result in this message.
        assertNull(messageRef.get());
    }
    
    @Subscribe
    public void onMessage(final MessageEvent event) {
        messageRef.set(event);
    }

}
