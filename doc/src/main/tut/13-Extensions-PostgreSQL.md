---
layout: book
number: 13
title: Extensions for PostgreSQL
---


### Array Types

Postgres typed arrays are supported and map to `Array`, `List`, `Vector`. Multi-dimensional arrays are not supported yet.

### Enum Types

### Geometric Types

The following geometric types are supported, and map to driver-supplied types. These will normally be mapped to application-specific types.
- the `box` schema type maps to `org.postgresql.geometric.PGbox`
- the `circle` schema type maps to `org.postgresql.geometric.PGcircle`
- the `lseg` schema type maps to `org.postgresql.geometric.PGlseg`
- the `path` schema type maps to `org.postgresql.geometric.PGpath`
- the `point` schema type maps to `org.postgresql.geometric.PGpoint`
- the `polygon` schema type maps to `org.postgresql.geometric.PGpolygon`

### PostGIS Types

This adds support for the main PostGIS types:

- `PGgeometry`
- `PGbox2d`
- `PGbox3d`

As well as the following abstract and fine-grained types carried by `PGgeometry`:

- `Geometry`
- `ComposedGeom`
- `GeometryCollection`
- `MultiLineString`
- `MultiPolygon`
- `PointComposedGeom`
- `LineString`
- `MultiPoint`
- `Polygon`
- `Point`

### Miscellaneous Nonstandard Types

- The `uuid` schema type is supported and maps to `java.util.UUID`.
- The `inet` schema type is supported and maps to `java.net.InetAddress`.

### Error Handling Extensions

A complete table of SQLSTATE values is provided in the `doobie.contrib.postgresql.sqlstate` module, and recovery combinators for each of these (`onUniqueViolation` for example) are provided in `doobie.contrib.postgresql.syntax`. 
