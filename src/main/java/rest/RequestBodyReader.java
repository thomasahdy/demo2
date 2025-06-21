package rest;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;

@Provider
@Priority(Priorities.ENTITY_CODER)
public class RequestBodyReader implements ContainerRequestFilter {

    private static ThreadLocal<String> requestBody = new ThreadLocal<>();

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (requestContext.getMethod().equalsIgnoreCase("POST")) {
            InputStream inputStream = requestContext.getEntityStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            byte[] bytes = baos.toByteArray();
            
            // Store the body for later access
            requestBody.set(new String(bytes));
            
            // Reset the input stream for further processing
            requestContext.setEntityStream(new ByteArrayInputStream(bytes));
        }
    }

    public static String getRequestBody() {
        return requestBody.get();
    }

    public static void clearRequestBody() {
        requestBody.remove();
    }
} 