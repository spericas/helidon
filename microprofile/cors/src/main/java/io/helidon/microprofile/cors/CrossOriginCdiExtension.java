

package io.helidon.microprofile.cors;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import java.io.IOException;
import java.io.InputStream;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;

import static javax.interceptor.Interceptor.Priority.PLATFORM_BEFORE;

public class CrossOriginCdiExtension implements Extension {

    void verifyCors(@Observes @Priority(PLATFORM_BEFORE) @Initialized(ApplicationScoped.class) Object event) {
        Index index = null;
        InputStream is = getClass().getClassLoader().getResourceAsStream("META-INF/jandex.idx");
        try {
            IndexReader reader = new IndexReader(is);
            index = reader.read();
        } catch (IOException e) {
            // TODO log index cannot be found or read
        }
    }
}
