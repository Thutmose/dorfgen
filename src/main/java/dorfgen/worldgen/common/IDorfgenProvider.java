package dorfgen.worldgen.common;

import dorfgen.conversion.DorfMap;

public interface IDorfgenProvider
{
    RiverMaker getRiverMaker();

    RoadMaker getRoadMaker();

    SiteMaker getSiteMaker();

    DorfMap getDorfMap();
}
