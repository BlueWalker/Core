package walker.blue.core.lib.ddb;

import com.amazonaws.services.dynamodbv2.model.GetItemResult;

import org.junit.Test;

import java.io.File;
import java.util.List;

import walker.blue.core.lib.types.Building;
import walker.blue.path.lib.GridNode;
import walker.blue.path.lib.ThetaStar;

/**
 * Unit test for AttrToJava class
 */
public class TestNewMap {

    private static final String CREDENTIALS_PATH = "../../../awscredentials";
    private static final String NOT_RUN = "Test was not run since the credentials file was not found";
    private static final String BUILDING_ID = "TEST_NEW_MAP";

    @Test
    public void testThatNewMap() {
        File credFile = new File(CREDENTIALS_PATH);
        if(!credFile.exists() || credFile.isDirectory()) {
            System.out.println(NOT_RUN);
            return;
        }
        DynamoDBWrapper ddb = new DynamoDBWrapper(AWSCredentialsLoader.loadCredentials(CREDENTIALS_PATH));
        GetItemResult data = ddb.getBuildingData(BUILDING_ID);
        Building building = AttrToJava.getItemResultToBuilding(data);
        final ThetaStar thetaStar = new ThetaStar();
        final List<List<GridNode>> fouthFloor = building.getSearchSpace().get(3);
        final List<GridNode> path = thetaStar.findPath(fouthFloor, fouthFloor.get(7).get(3), fouthFloor.get(9).get(24));
        for (GridNode node : path) {
            System.out.println(node.toString());
        }
    }
}
