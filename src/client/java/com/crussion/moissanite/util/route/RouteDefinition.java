package com.crussion.moissanite.util.route;

import java.util.ArrayList;
import java.util.List;

public final class RouteDefinition {
	public String name = "";
	public boolean active;
	public List<RouteAction> actions = new ArrayList<>();

	public RouteDefinition() {
	}

	public RouteDefinition(String name) {
		this.name = name;
	}
}
