/**
 * Entry / delivery layer for the personal finance backend.
 *
 * <p>This is the only layer that depends on Spring. It adapts HTTP requests to application use
 * cases and wires the Core (domain, application, infrastructure) through explicit
 * {@code @Configuration} / {@code @Bean} definitions. Core packages must stay free of Spring
 * annotations and imports.
 *
 * @author Raul
 */
package com.rauldev.personalfinance.entry;
