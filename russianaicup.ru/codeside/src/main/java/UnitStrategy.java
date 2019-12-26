import model.Game;
import model.Unit;
import model.UnitAction;
import model.Vec2Double;
import strategy.world.World;

public interface UnitStrategy {

	Vec2Double ZERO = new Vec2Double(0, 0);
	UnitAction NO_ACTION = new UnitAction(0, false, false, ZERO, false,
			false, false, false);

	UnitAction getUnitAction(World world, Unit unit, Game game, Debug debug, Unit before, StrategyAttackRecovery manager);
	boolean feasible(World world, Unit unit, Game game, Debug debug, Unit before);
}
