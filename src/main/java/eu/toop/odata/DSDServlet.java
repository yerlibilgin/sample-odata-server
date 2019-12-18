package eu.toop.odata;

import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

public class DSDServlet extends HttpServlet {
  private static final Logger LOGGER = LoggerFactory.getLogger(DSDServlet.class);

  private static final DiscoveryCollectionProcessor dcp = new DiscoveryCollectionProcessor();
  private static final DiscoveryEntityProcessor dep = new DiscoveryEntityProcessor();
  private static final DiscoveryPrimitiveProcessor dpp = new DiscoveryPrimitiveProcessor();
  private static final ErrorProcessor ep = new ErrorProcessor();

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
    try {
      OData odata = OData.newInstance();
      ServiceMetadata edm = odata.createServiceMetadata(new DiscoveryEdmProvider(), new ArrayList<>());
      ODataHttpHandler handler = odata.createHandler(edm);
      handler.register(dcp);
      handler.register(dep);
      handler.register(dpp);
      handler.register(ep);
      handler.process(req, resp);
    } catch (RuntimeException e) {
      LOGGER.error("Server Error", e);
      throw new ServletException(e);
    }
  }
}
