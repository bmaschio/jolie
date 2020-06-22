package joliex.openapi.impl.support;

import jolie.runtime.FaultException;
import jolie.runtime.Value;
import joliex.util.JsonUtilsService;
import junit.framework.TestCase;

public class RestDescriptorTest extends TestCase {

    public void testLoadDescriptor() throws FaultException {
        RestDescriptor restDescriptor = new RestDescriptor();
        try {
            restDescriptor.loadDescriptor("rest_template.json");
        } catch (FaultException e) {
            if ("ValidationApiMapper".equals(e.faultName())){
                JsonUtilsService jsonUtilsService = new JsonUtilsService();
                Value v = jsonUtilsService.getJsonString(e.value());
                System.err.println(v.strValue());
            }
        }
    }
}