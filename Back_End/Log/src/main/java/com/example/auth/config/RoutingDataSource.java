package com.example.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class RoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(RoutingDataSource.class);

    @Override
    protected Object determineCurrentLookupKey() {
        boolean readOnlyTransaction = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        String lookupKey = readOnlyTransaction ? "read" : "write";

        if (log.isDebugEnabled()) {
            log.debug("Routing database request to {} datasource", lookupKey);
        }

        return lookupKey;
    }
}
