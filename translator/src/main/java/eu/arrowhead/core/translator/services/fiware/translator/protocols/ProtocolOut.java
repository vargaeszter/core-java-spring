package eu.arrowhead.core.translator.services.translator.protocols;

import java.net.URI;

public class ProtocolOut {

    final URI uri;
    ProtocolIn protocolIn;

    public ProtocolOut(URI uri) {
        this.uri = uri;
    }

    public void setProtocolIn(ProtocolIn protocolIn) {
        this.protocolIn = protocolIn;
    }

    public InterProtocolResponse get(InterProtocolRequest request) {
        return new InterProtocolResponse();
    }

    public InterProtocolResponse post(InterProtocolRequest request) {
        return new InterProtocolResponse();
    }

    public InterProtocolResponse put(InterProtocolRequest request) {
        return new InterProtocolResponse();
    }

    public InterProtocolResponse patch(InterProtocolRequest request) {
        return new InterProtocolResponse();
    }

    public InterProtocolResponse delete(InterProtocolRequest request) {
        return new InterProtocolResponse();
    }

    public InterProtocolResponse observe(InterProtocolRequest request) {
        return new InterProtocolResponse();
    }

}

