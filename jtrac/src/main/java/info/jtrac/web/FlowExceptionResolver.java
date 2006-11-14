/*
 * Copyright 2002-2005 the original author or authors.
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

package info.jtrac.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.webflow.execution.repository.NoSuchFlowExecutionException;
import org.springframework.webflow.execution.repository.PermissionDeniedFlowExecutionAccessException;
import org.springframework.webflow.executor.support.FlowExecutorArgumentExtractionException;

/**
 * Spring MVC exception handler resolver designed to gracefully handle
 * the case where the user hit the browser back button during a WebFlow
 */
public class FlowExceptionResolver implements HandlerExceptionResolver {
    
    protected final Log logger = LogFactory.getLog(getClass());
    
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object o, Exception e) {
        logger.debug("exception type: " + e.getClass() + ", message: " + e.getMessage());
        if (e instanceof NoSuchFlowExecutionException 
                || e instanceof FlowExecutorArgumentExtractionException) {
            logger.debug("session must have expired in the middle of a flow, redirecting to dashboard");            
            return new ModelAndView("redirect:/app");
        } else if (e instanceof PermissionDeniedFlowExecutionAccessException) {
            logger.debug("user must have hit browser back button, trying to handle gracefully");        
            return new ModelAndView("exception_flow");
        } else {
            return null;  // not an exception we are interested in, pass control back to Spring
        }       
    }
    
}