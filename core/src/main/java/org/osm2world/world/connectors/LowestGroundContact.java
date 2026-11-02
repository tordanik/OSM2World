package org.osm2world.world.connectors;

import java.util.List;

import org.osm2world.math.shapes.PolygonShapeXZ;

/**
 * requires a connector's elevation to be the lowest ground elevation along one or more outlines.
 * This is used for features such as buildings, whose heights are measured from the lowest point where their
 * outline has contact with the ground.
 * <p>
 * The connector itself is typically {@link GroundState#DERIVED}, and its position does not matter.
 *
 * @param connector  the connector whose elevation is determined by this relation
 * @param outlines  the outlines along which the ground elevation is considered, including any holes
 */
public record LowestGroundContact(Connector connector, List<? extends PolygonShapeXZ> outlines)
		implements EleRelation {

	public LowestGroundContact {
		if (outlines.isEmpty()) throw new IllegalArgumentException("no outlines");
		outlines = List.copyOf(outlines);
	}

}
