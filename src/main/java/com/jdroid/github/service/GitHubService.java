/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package com.jdroid.github.service;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static com.jdroid.github.client.PagedRequest.PAGE_FIRST;
import static com.jdroid.github.client.PagedRequest.PAGE_SIZE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.jdroid.github.IRepositoryIdProvider;
import com.jdroid.github.client.GitHubClient;
import com.jdroid.github.client.GitHubRequest;
import com.jdroid.github.client.NoSuchPageException;
import com.jdroid.github.client.PageIterator;
import com.jdroid.github.client.PagedRequest;
import com.jdroid.github.client.RequestException;

/**
 * Base GitHub service class.
 */
public abstract class GitHubService {

	/**
	 * Accept header for raw response (only body)
	 */
	public static final String ACCEPT_RAW = "application/vnd.github.v3.raw+json"; //$NON-NLS-1$

	/**
	 * Accept header for HTML response (only bodyHtml)
	 */
	public static final String ACCEPT_HTML = "application/vnd.github.v3.html+json"; //$NON-NLS-1$

	/**
	 * Accept header for text response (only bodyText)
	 */
	public static final String ACCEPT_TEXT = "application/vnd.github.v3.text+json"; //$NON-NLS-1$

	/**
	 * Accept header for full response (body, bodyText and bodyHtml)
	 */
	public static final String ACCEPT_FULL = "application/vnd.github.v3.full+json"; //$NON-NLS-1$

	/**
	 * Client field
	 */
	protected final GitHubClient client;

	/**
	 * Create service using a default {@link GitHubClient}
	 */
	public GitHubService() {
		this(new GitHubClient());
	}

	/**
	 * Create service for client
	 *
	 * @param client
	 *            must be non-null
	 */
	public GitHubService(GitHubClient client) {
		if (client == null)
			throw new IllegalArgumentException("Client cannot be null"); //$NON-NLS-1$
		this.client = client;
	}

	/**
	 * Get configured client
	 *
	 * @return non-null client
	 */
	public GitHubClient getClient() {
		return client;
	}

	/**
	 * Unified request creation method that all sub-classes should use so
	 * overriding classes can extend and configure the default request.
	 *
	 * @return request
	 */
	protected GitHubRequest createRequest() {
		return new GitHubRequest();
	}

	/**
	 * Unified paged request creation method that all sub-classes should use so
	 * overriding classes can extend and configure the default request.
	 *
	 * @return request
	 */
	protected <V> PagedRequest<V> createPagedRequest() {
		return createPagedRequest(PAGE_FIRST, PAGE_SIZE);
	}

	/**
	 * Unified paged request creation method that all sub-classes should use so
	 * overriding classes can extend and configure the default request.
	 *
	 * @param start
	 * @param size
	 * @return request
	 */
	protected <V> PagedRequest<V> createPagedRequest(int start, int size) {
		return new PagedRequest<V>(start, size);
	}

	/**
	 * Unified page iterator creation method that all sub-classes should use so
	 * overriding classes can extend and configure the default iterator.
	 *
	 * @param request
	 * @return iterator
	 */
	protected <V> PageIterator<V> createPageIterator(PagedRequest<V> request) {
		return new PageIterator<V>(request, client);
	}

	/**
	 * Get paged request by performing multiple requests until no more pages are
	 * available or an exception occurs.
	 *
	 * @param <V>
	 * @param request
	 * @return list of all elements
	 * @throws IOException
	 */
	protected <V> List<V> getAll(PagedRequest<V> request) throws IOException {
		return getAll(createPageIterator(request));
	}

	/**
	 * Get paged request by performing multiple requests until no more pages are
	 * available or an exception occurs.
	 *
	 * @param <V>
	 * @param iterator
	 * @return list of all elements
	 * @throws IOException
	 */
	protected <V> List<V> getAll(PageIterator<V> iterator) throws IOException {
		List<V> elements = new ArrayList<V>();
		try {
			while (iterator.hasNext())
				elements.addAll(iterator.next());
		} catch (NoSuchPageException pageException) {
			throw pageException.getCause();
		}
		return elements;
	}

	/**
	 * Check if the uri returns a non-404
	 *
	 * @param uri
	 * @return true if no exception, false if 404
	 * @throws IOException
	 */
	protected boolean check(String uri) throws IOException {
		try {
			client.get(createRequest().setUri(uri));
			return true;
		} catch (RequestException e) {
			if (e.getStatus() == HTTP_NOT_FOUND)
				return false;
			throw e;
		}
	}

	/**
	 * Get id for repository
	 *
	 * @param provider
	 * @return non-null id
	 */
	protected String getId(IRepositoryIdProvider provider) {
		if (provider == null)
			throw new IllegalArgumentException(
					"Repository provider cannot be null"); //$NON-NLS-1$
		final String id = provider.generateId();
		if (id == null)
			throw new IllegalArgumentException("Repository id cannot be null"); //$NON-NLS-1$
		if (id.length() == 0)
			throw new IllegalArgumentException("Repository id cannot be empty"); //$NON-NLS-1$
		return id;
	}

	/**
	 * Verify user and repository name
	 *
	 * @param user
	 * @param repository
	 * @return this service
	 */
	protected GitHubService verifyRepository(String user, String repository) {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$
		if (repository == null)
			throw new IllegalArgumentException("Repository cannot be null"); //$NON-NLS-1$
		if (repository.length() == 0)
			throw new IllegalArgumentException("Repository cannot be empty"); //$NON-NLS-1$
		return this;
	}
}