package com.eginnovations.support.pmr;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.eg.api.client.EgRequestHeader;
import com.eg.api.client.dao.EgComponentDao;
import com.eg.api.client.entity.ManagedComponent;
import com.eg.api.client.exception.InvalidRequestHeaderException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
/**
 * Service class to handle inventory-related operations, such as fetching eG Agents from the eG Manager.
 * @author Pandian
 * @since 2026-02-14
 */
@Service
public class InventoryService {
	Logger logger = LoggerFactory.getLogger(InventoryService.class);
	private EgComponentDao egComponentDao = new EgComponentDao();
	
	/**
	 * Fetches the list of eG Agents (components) from the eG Manager based on the provided request header.
	 *
	 * @param egRequestHeader The request header containing necessary information for authentication and authorization.
	 * @return A list of ManagedComponent objects representing the eG Agents. If no components are found, returns an empty list.
	 * @throws JsonMappingException If there is an error mapping the JSON response to Java objects.
	 * @throws JsonProcessingException If there is an error processing the JSON response.
	 * @throws InvalidRequestHeaderException If the provided request header is invalid or missing required information.
	 */
	public List<ManagedComponent> getComponents(EgRequestHeader egRequestHeader) throws JsonMappingException, JsonProcessingException, InvalidRequestHeaderException {
		List<ManagedComponent> components = egComponentDao.showComponents(egRequestHeader, "All");
		if (components == null || components.isEmpty()) {
			logger.info("No components found.");
			return List.of();
		}
		logger.info("Found {} components", components.size());
		return components;
	}
}
