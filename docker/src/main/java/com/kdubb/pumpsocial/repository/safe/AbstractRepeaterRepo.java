package com.kdubb.pumpsocial.repository.safe;

import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.kdubb.pumpsocial.service.RepeaterService;

public abstract class AbstractRepeaterRepo<T, ID extends Serializable> implements MongoRepository<T, ID> {

	@Inject
	protected RepeaterService repeaterService;

	protected abstract MongoRepository<T, ID> getRepo();

	@Override
	public T findOne(final ID id) {
		if (id == null)
			return null;

		return repeaterService.retryIfNecessary(() -> getRepo().findOne(id));
	}

	@Override
	public List<T> findAll() {
		return repeaterService.retryIfNecessary(() -> getRepo().findAll());
	}

	@Override
	public Page<T> findAll(final Pageable pageable) {
		if (pageable == null)
			return null;

		return repeaterService.retryIfNecessary(() -> getRepo().findAll(pageable));
	}

	@Override
	public <S extends T> S save(final S entity) {
		if (entity == null)
			return null;

		return repeaterService.retryIfNecessary(() -> getRepo().save(entity));
	}

	@Override
	public boolean exists(final ID id) {
		if (id == null)
			return false;

		return repeaterService.retryIfNecessary(() -> {
			return getRepo().exists(id);
		});
	}

	@Override
	public Iterable<T> findAll(final Iterable<ID> ids) {
		if (ids == null)
			return null;

		return repeaterService.retryIfNecessary(() -> getRepo().findAll(ids));
	}

	@Override
	public long count() {
		return repeaterService.retryIfNecessary(() -> {
			return getRepo().count();
		});
	}

	@Override
	public void delete(final ID id) {
		if (id == null)
			return;

		repeaterService.retryIfNecessary(() -> {
			getRepo().delete(id);
			return true;
		});
	}

	@Override
	public void delete(final T entity) {
		if (entity == null)
			return;

		repeaterService.retryIfNecessary(() -> {
			getRepo().delete(entity);
			return true;
		});
	}

	@Override
	public void delete(final Iterable<? extends T> entities) {
		if (entities == null)
			return;

		repeaterService.retryIfNecessary(() -> {
			getRepo().delete(entities);
			return true;
		});
	}

	@Override
	public void deleteAll() {
		repeaterService.retryIfNecessary(() -> {
			getRepo().deleteAll();
			return true;
		});
	}

	@Override
	public <S extends T> List<S> save(final Iterable<S> entities) {
		if (entities == null)
			return null;

		return repeaterService.retryIfNecessary(() -> getRepo().save(entities));
	}

	@Override
	public List<T> findAll(final Sort sort) {
		if (sort == null)
			return null;

		return repeaterService.retryIfNecessary(() -> getRepo().findAll(sort));
	}
}