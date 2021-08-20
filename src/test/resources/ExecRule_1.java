
import io.openslice.osom.lcm.LcmBaseExecutor;


public class ExecRule_1 extends LcmBaseExecutor{
	
@Override
public void exec() {
//SNIP STARTS
if (getCharValNumber("Quality Class") == 2) {
  setCharValString("cirros_2vnf_ns::OSM_CONFIG", """
  {
      "nsdId": "0b6853fc-8219-4580-9697-bf4a8f0a08f9",
      "vimAccountId": "c224eb48-419e-4097-8a1d-11ec1bba087f"
  }
  """);
} else if (getCharValNumber("Quality Class") == 1) {
  setCharValString("cirros_2vnf_ns::OSM_CONFIG", """
  {
      "nsdId": "eeeeeeee-8219-4580-9697-bf4a8f0a08f9",
      "vimAccountId": "eeeeeeee-419e-4097-8a1d-11ec1bba087f"
  }
  """);
} else {
  setCharValString("cirros_2vnf_ns::OSM_CONFIG", """
  {
      "nsdId": "cccccccc-8219-4580-9697-bf4a8f0a08f9",
      "vimAccountId": "cccccccc-419e-4097-8a1d-11ec1bba087f"
  }
  """);
}
		//SNIP ENDS
		}
	}

