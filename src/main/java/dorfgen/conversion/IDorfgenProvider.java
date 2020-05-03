package dorfgen.conversion;

import dorfgen.world.feature.RiverMaker;
import dorfgen.world.feature.RoadMaker;
import dorfgen.world.feature.SiteMaker;

public interface IDorfgenProvider
{
    RiverMaker getRiverMaker();

    RoadMaker getRoadMaker();

    SiteMaker getSiteMaker();

    DorfMap getDorfMap();
}