package com.kdubb.pumpsocial.domain;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.util.CollectionUtils;

@Document
public class Route extends MongoBase {

	@DBRef
	private SocialConnection source;

	@DBRef
	private Set<SocialConnection> targets;

	public Set<SocialConnection> getTargets() {
		return targets;
	}

	public void setTargets(Set<SocialConnection> targets) {
		this.targets = targets;
	}

	public SocialConnection getSource() {
		return source;
	}

	public void setSource(SocialConnection source) {
		this.source = source;
	}

	public void merge(Route existing) {
		merge(existing, true);
	}

	public void merge(Route existing, boolean addativeTargets) {
		if (source != null)
			existing.setSource(source);

		if (!CollectionUtils.isEmpty(targets)) {
			if (!addativeTargets || CollectionUtils.isEmpty(existing.getTargets())) {
				existing.setTargets(targets);
			} else {
				Set<String> ids = existing.getTargets().stream() //
						.filter(x -> x != null && x.getId() != null)
						.map(x -> x.getId().toString())
						.collect(Collectors.toSet());

				for (SocialConnection target : targets) {
					String id = target.getId().toString();

					if (!ids.contains(id)) {
						ids.add(id);
						existing.getTargets().add(target);
					}
				}
			}
		}
	}
}