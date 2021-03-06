package org.moskito.controlagent.endpoints.http;

import net.anotheria.util.StringUtils;
import org.moskito.controlagent.Agent;
import org.moskito.controlagent.data.accumulator.AccumulatorHolder;
import org.moskito.controlagent.data.accumulator.AccumulatorListItem;
import org.moskito.controlagent.data.status.ThresholdStatusHolder;
import org.moskito.controlagent.data.threshold.ThresholdDataItem;
import org.moskito.controlagent.endpoints.EndpointUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Provides data replying to MoSKito Control HTTP-requests.
 *
 * @author lrosenberg
 * @since 15.04.13 20:43
 */
@WebFilter(description = "MoSKito Control Endpoint", filterName = "MoSKitoControlEndpoint",
		urlPatterns = {
		"/moskito-control-agent/*"
	})
public class HttpEndpoint implements Filter {

	private static Logger log = LoggerFactory.getLogger(HttpEndpoint.class);

	static enum COMMAND{
		/**
		 * Requests the status of this component. The status is calculated based on worst threshold.
		 */
		STATUS,
		/**
		 * Requests the list of the accumulators.
		 */
		ACCUMULATORS,
		/**
		 * Requests the data for one or multiple accumulators.
		 */
		ACCUMULATOR,
        /**
         * Requests the data for thresholds of this component.
         */
        THRESHOLDS
	};

	public static final String MAPPED_NAME = "moskito-control-agent";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest)servletRequest;
		String requestURI = req.getRequestURI();
		//we ignore the case that request uri could be null
		boolean handled = false;
		String myPath = requestURI.substring(requestURI.indexOf(MAPPED_NAME) +MAPPED_NAME.length() + 1);
		String tokens[] = StringUtils.tokenize(myPath, '/');
		COMMAND command = COMMAND.valueOf(tokens[0].toUpperCase());
		switch(command){
			case STATUS:
				status(servletRequest, servletResponse, tokens);
				break;
			case ACCUMULATORS:
				accumulators(servletRequest, servletResponse, tokens);
				break;
			case ACCUMULATOR:
				accumulator(servletRequest, servletResponse, tokens);
				break;
            case THRESHOLDS:
                thresholds(servletRequest, servletResponse, tokens);
                break;
			default:
				throw new AssertionError("Unrecognized command "+command);
		}
	}

    private void status(ServletRequest servletRequest, ServletResponse servletResponse, String parameters[]) throws IOException{
        ThresholdStatusHolder status = Agent.getInstance().getThresholdStatus();
        writeReply(servletResponse, status);

    }

    private void accumulators(ServletRequest servletRequest, ServletResponse servletResponse, String parameters[]) throws IOException{
		List<AccumulatorListItem> ret = Agent.getInstance().getAvailableAccumulators();
		writeReply(servletResponse, ret);
	}

	private void accumulator(ServletRequest servletRequest, ServletResponse servletResponse, String parameters[]) throws IOException{
		if (parameters.length==1)
			throw new IllegalArgumentException("No accumulators specified");
		LinkedList<String> names = new LinkedList<String>();
		for (int i=1; i<parameters.length; i++){
			names.add(parameters[i]);
		}
		Map<String, AccumulatorHolder> accumulators = Agent.getInstance().getAccumulatorsData(names);
		writeReply(servletResponse, accumulators);
	}

    private void thresholds(ServletRequest servletRequest, ServletResponse servletResponse, String parameters[]) throws IOException {
        List<ThresholdDataItem> thresholds = Agent.getInstance().getThresholds();
        writeReply(servletResponse, thresholds);
    }

	void writeReply(ServletResponse servletResponse, Object parameter) throws IOException{
		byte[] data = EndpointUtility.object2JSON(parameter);
		OutputStream out = servletResponse.getOutputStream();
		out.write(data);
		out.flush();
		out.close();
	}

	@Override
	public void destroy() {
	}

}
