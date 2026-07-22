wit_bindgen::generate!({
    path: "../../arcade-player/world.wit",
    world: "player",
});

use crate::exports::bnorm::arcade::driver::{Car, Guest, Race};
use bnorm::arcade::controls;

struct Farris;

impl Guest for Farris {
    fn on_race(_race: Race) -> () {}

    fn on_turn(_car: Car) -> () {
        // Just drive in a circle...
        controls::throttle_set(0.1);
        controls::steering_set(1.0);
    }

    fn on_draw() -> () {}
}

__export_player_impl!(Farris);
