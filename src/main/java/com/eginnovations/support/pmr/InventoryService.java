package com.eginnovations.support.pmr;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.eg.api.client.EgRequestHeader;
import com.eg.api.client.dao.EgComponentDao;
import com.eg.api.client.entity.ManagedComponent;
import com.eg.api.client.exception.InvalidRequestHeaderException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
/**
 * Service class to handle inventory-related operations, such as fetching eG Agents from the eG Manager.
 */
@Service
public class InventoryService {
	Logger logger = LoggerFactory.getLogger(InventoryService.class);
	private EgComponentDao egComponentDao = new EgComponentDao();
	
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
