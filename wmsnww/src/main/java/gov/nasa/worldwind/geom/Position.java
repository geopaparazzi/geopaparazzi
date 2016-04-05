/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.geom;

import gov.nasa.worldwind.util.Logging;

import java.util.*;

/**
 * @author dcollins
 * @version $Id: Position.java 829 2012-10-08 20:42:03Z tgaskins $
 */
public class Position extends LatLon
{
    public static final Position ZERO = new Position();

    public double elevation;

    public Position()
    {
    }

    public Position(Angle latitude, Angle longitude, double elevation)
    {
        super(latitude, longitude);
        this.elevation = elevation;
    }

    public Position(LatLon location, double elevation)
    {
        super(location.latitude.copy(), location.longitude.copy());
        this.elevation = elevation;
    }

    public static Position fromDegrees(double latitude, double longitude, double elevation)
    {
        return new Position(Angle.fromDegrees(latitude), Angle.fromDegrees(longitude), elevation);
    }

    public static Position fromDegrees(double latitude, double longitude)
    {
        return new Position(Angle.fromDegrees(latitude), Angle.fromDegrees(longitude), 0);
    }

    public static Position fromRadians(double latitude, double longitude, double elevation)
    {
        return new Position(Angle.fromRadians(latitude), Angle.fromRadians(longitude), elevation);
    }

    /**
     * Returns the an interpolated location along the great-arc between the specified positions. This does not retain
     * any reference to the specified positions, or modify them in any way.
     * <p/>
     * The interpolation factor amount is a floating-point value in the range [0.0, 1.0] which defines the weight given
     * to each position. The position's elevation components are linearly interpolated as a simple scalar value.
     *
     * @param amount the interpolation factor as a floating-point value in the range [0.0, 1.0].
     * @param lhs    the first position.
     * @param rhs    the second position.
     *
     * @return an interpolated position along the great-arc between lhs and rhs.
     *
     * @throws IllegalArgumentException if either position is <code>null</code>.
     */
    public static Position interpolateGreatCircle(double amount, Position lhs, Position rhs)
    {
        if (lhs == null)
        {
            String msg = Logging.getMessage("nullValue.LhsIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (rhs == null)
        {
            String msg = Logging.getMessage("nullValue.RhsIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        double t = (amount < 0 ? 0 : (amount > 1 ? 1 : amount));

        LatLon latLon = LatLon.interpolateGreatCircle(t, lhs, rhs);
        double elevation = lhs.elevation + t * (rhs.elevation - lhs.elevation);

        return new Position(latLon, elevation);
    }

    /**
     * Returns the an interpolated location along the rhumb line between the specified positions. This does not retain
     * any reference to the specified positions, or modify them in any way.
     * <p/>
     * The interpolation factor amount is a floating-point value in the range [0.0, 1.0] which defines the weight given
     * to each position. The position's elevation components are linearly interpolated as a simple scalar value.
     *
     * @param amount the interpolation factor as a floating-point value in the range [0.0, 1.0].
     * @param lhs    the first position.
     * @param rhs    the second position.
     *
     * @return an interpolated position along the rhumb line between lhs and rhs.
     *
     * @throws IllegalArgumentException if either position is <code>null</code>.
     */
    public static Position interpolateRhumb(double amount, Position lhs, Position rhs)
    {
        if (lhs == null)
        {
            String msg = Logging.getMessage("nullValue.LhsIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (rhs == null)
        {
            String msg = Logging.getMessage("nullValue.RhsIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        double t = (amount < 0 ? 0 : (amount > 1 ? 1 : amount));

        LatLon latLon = LatLon.interpolateRhumb(t, lhs, rhs);
        double elevation = lhs.elevation + t * (rhs.elevation - lhs.elevation);

        return new Position(latLon, elevation);
    }

    /**
     * Computes a new set of positions translated from a specified reference position to a new reference position. This
     * does not retain any reference to the specified positions, or modify them in any way.
     * <p/>
     * The returned list contains the same number of positions as the original iterable, which have been adjusted from
     * their original counterparts by the difference between the specified old and new positions. Each new location is
     * placed along a great circle arc starting at the new position, but with the same azimuth and distance as the great
     * circle arc between the old position and the location. Each new position's elevation is adjusted by the difference
     * in elevation between the old position and the new position.
     *
     * @param oldPosition the original reference position.
     * @param newPosition the new reference position.
     * @param iterable    the positions to translate.
     *
     * @return a list containing the translated positions.
     *
     * @throws IllegalArgumentException if either of the positions or the iterable is <code>null</code>.
     */
    public static List<Position> translatePositions(Position oldPosition, Position newPosition,
        Iterable<? extends Position> iterable)
    {
        if (oldPosition == null)
        {
            String msg = Logging.getMessage("nullValue.OldPositionIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (newPosition == null)
        {
            String msg = Logging.getMessage("nullValue.NewPositionIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (iterable == null)
        {
            String msg = Logging.getMessage("nullValue.IterableIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // TODO: Modify the list of positions in place instead of returning a list of new positions.
        // TODO: Account for dateline spanning
        ArrayList<Position> newPositions = new ArrayList<Position>();

        double elevDelta = newPosition.elevation - oldPosition.elevation;

        for (Position pos : iterable)
        {
            Angle distance = LatLon.greatCircleDistance(oldPosition, pos);
            Angle azimuth = LatLon.greatCircleAzimuth(oldPosition, pos);
            LatLon newLocation = LatLon.greatCircleEndPosition(newPosition, azimuth, distance);
            double newElev = pos.elevation + elevDelta;

            newPositions.add(new Position(newLocation, newElev));
        }

        return newPositions;
    }

    public Position copy()
    {
        return new Position(this.latitude.copy(), this.longitude.copy(), this.elevation);
    }

    public Position set(Position position)
    {
        if (position == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        super.set(position.latitude, position.longitude);
        this.elevation = position.elevation;

        return this;
    }

    public Position set(LatLon location, double elevation)
    {
        if (location == null)
        {
            String msg = Logging.getMessage("nullValue.LocationIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        super.set(location.latitude, location.longitude);
        this.elevation = elevation;

        return this;
    }

    public Position set(Angle latitude, Angle longitude, double elevation)
    {
        if (latitude == null)
        {
            String msg = Logging.getMessage("nullValue.LatitudeIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (longitude == null)
        {
            String msg = Logging.getMessage("nullValue.LongitudeIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        super.set(latitude, longitude);
        this.elevation = elevation;

        return this;
    }

    // A class that makes it easier to pass around position lists.
    public static class PositionList
    {
        public List<? extends Position> list;

        public PositionList(List<? extends Position> list)
        {
            this.list = list;
        }
    }

    public Position setDegrees(double latitude, double longitude, double elevation)
    {
        super.setDegrees(latitude, longitude);
        this.elevation = elevation;

        return this;
    }

    public Position setRadians(double latitude, double longitude, double elevation)
    {
        super.setRadians(latitude, longitude);
        this.elevation = elevation;

        return this;
    }

    public Position add(Position that)
    {
        double lat = Angle.normalizedDegreesLatitude(this.latitude.degrees + that.latitude.degrees);
        double lon = Angle.normalizedDegreesLongitude(this.longitude.degrees + that.longitude.degrees);

        return Position.fromDegrees(lat, lon, this.elevation + that.elevation);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || this.getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        Position that = (Position) o;
        return this.elevation == that.elevation;
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        long temp;
        temp = this.elevation != +0.0d ? Double.doubleToLongBits(this.elevation) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(this.latitude.toString()).append(", ");
        sb.append(this.longitude.toString()).append(", ");
        sb.append(this.elevation);
        sb.append(")");
        return sb.toString();
    }
}
