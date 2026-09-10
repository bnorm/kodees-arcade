use crate::bnorm::arcade::geometry::Point;
use crate::bnorm::arcade::types::Track;
use crate::exports::bnorm::arcade::driver::{Car, Guest, Race};
use bnorm::arcade::controls;
use std::cell::RefCell;
use std::f64::consts::PI;

wit_bindgen::generate!({
    path: "../../arcade-player/world.wit",
    world: "player",
    additional_derives: [Clone],
});

impl Default for Track {
    fn default() -> Self {
        Track {
            width: 0.0,
            height: 0.0,
            checkpoints: vec![],
            positions: vec![],
        }
    }
}

struct Farris;

thread_local! {
    static TRACK: RefCell<Track> = RefCell::new(Track::default());
}

impl Guest for Farris {
    fn on_race(race: Race) -> () {
        TRACK.set(race.track);
    }

    fn on_turn(car: Car) -> () {
        let track = TRACK.with(|c| c.borrow().clone());
        let next = track.checkpoints.get(car.next_checkpoint as usize).unwrap();
        let target = Point {
            x: (next.start.x + next.end.x) / 2.0,
            y: (next.start.y + next.end.y) / 2.0,
        };

        // Go a safe speed... for now!
        controls::throttle_set(0.4);

        // Figure out how to steer.

        // Find the bearing (-180..180) to the target checkpoint.
        let target_heading = (target.y - car.location.y).atan2(target.x - car.location.x);
        let mut bearing = target_heading - car.velocity.angle.radians;
        bearing = (bearing + PI) % (2.0 * PI);
        if bearing > 0.0 {
            bearing -= PI;
        } else {
            bearing += PI;
        }

        if bearing == 0.0 {
            controls::steering_set(0.0);
        } else {
            controls::steering_set(bearing.signum() * 1.0);
        }
        // TODO replicate the 'else' block here.
        /*
        controls.steering = when {
            turn == Angle.ZERO -> 0.0
            speed == 0.0 -> sign(turn) * MAX_STEER
            else -> {
                val maxTurn = getTurn(speed, steering = MAX_STEER, traction = 1.0)
                (turn / maxTurn).coerceIn(MIN_STEER, MAX_STEER)
            }
        }
         */
    }

    fn on_car(car: Car) -> () {}

    fn on_draw() -> () {}
}

__export_player_impl!(Farris);
