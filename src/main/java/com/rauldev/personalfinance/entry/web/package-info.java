/**
 * HTTP controllers of the entry layer.
 *
 * <p>Rules for controllers placed in this package (or sub-packages per feature):
 * <ul>
 *   <li>Controllers only call application use cases or application query ports; they never use
 *       repositories, JDBC classes, or {@code TransactionConnectionHolder} directly.</li>
 *   <li>Transactions are handled by the use cases through the application
 *       {@code TransactionManager}; do not use Spring's {@code @Transactional}.</li>
 *   <li>Request and response bodies are web DTOs defined in the entry layer. They are mapped to
 *       application commands/queries and from application read models; read models and domain
 *       objects are not serialized directly.</li>
 *   <li>The user identity comes only from {@code entry.security.CurrentUserProvider}, never from
 *       the request (path, query, header, or body). The application currently runs in
 *       single-user mode ({@code ConfiguredSingleUserProvider}); real authentication will replace
 *       only that bean.</li>
 *   <li>Required request fields are checked with {@code entry.web.common.RequiredFields} before a
 *       command is built, so a missing field answers 400 instead of 500. Money is exchanged as
 *       integer cents ({@code entry.web.common.MoneyCents}).</li>
 *   <li>Errors are translated to RFC 9457 problem details in {@code entry.web.error}.</li>
 *   <li>Dependencies point inward: domain, application, and infrastructure never depend on
 *       Spring or on this package.</li>
 * </ul>
 */
package com.rauldev.personalfinance.entry.web;
