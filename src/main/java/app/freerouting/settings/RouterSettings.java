package app.freerouting.settings;

import app.freerouting.autoroute.BoardUpdateStrategy;
import app.freerouting.autoroute.ItemSelectionStrategy;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class RouterSettings implements Serializable
{
  @SerializedName("max_passes")
  public int maxPasses = 100;
  @SerializedName("max_threads")
  public int maxThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
  @SerializedName("improvement_threshold")
  public float optimizationImprovementThreshold = 0.01f;
  public transient BoardUpdateStrategy boardUpdateStrategy = BoardUpdateStrategy.GREEDY;
  public transient String hybridRatio = "1:1";
  public transient ItemSelectionStrategy itemSelectionStrategy = ItemSelectionStrategy.PRIORITIZED;
  @SerializedName("ignore_net_classes")
  public transient String[] ignoreNetClassesByAutorouter = new String[0];
  public transient AutorouteSettings autorouterSettings = new AutorouteSettings(0);
}