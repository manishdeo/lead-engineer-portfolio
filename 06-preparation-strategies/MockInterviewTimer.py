import time
import sys

def run_timer(minutes, phase_name):
    seconds = minutes * 60
    print(f"\n--- Starting Phase: {phase_name} ({minutes} minutes) ---")
    
    try:
        while seconds > 0:
            mins, secs = divmod(seconds, 60)
            timeformat = '{:02d}:{:02d}'.format(mins, secs)
            sys.stdout.write(f"\rTime remaining: {timeformat}")
            sys.stdout.flush()
            time.sleep(1)
            seconds -= 1
        print(f"\n\nTime's up for {phase_name}!")
    except KeyboardInterrupt:
        print("\nTimer interrupted.")

if __name__ == "__main__":
    print("Welcome to the System Design Mock Interview Timer")
    print("This follows the RACED framework (Requirements, Architecture, Components, Estimate, Deep-dive)")
    
    phases = [
        ("Requirements & Scope", 5),
        ("Capacity Estimation", 5),
        ("High-Level Architecture", 10),
        ("Component Deep Dive", 15),
        ("Trade-offs & Bottlenecks", 10)
    ]
    
    input("Press Enter to start the mock interview...")
    
    for name, duration in phases:
        run_timer(duration, name)
        if name != phases[-1][0]:
            input("\nPress Enter to move to the next phase...")
            
    print("\nMock interview complete. Great job!")
