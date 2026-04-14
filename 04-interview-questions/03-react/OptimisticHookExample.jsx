import React, { useState, useOptimistic } from 'react';

/**
 * Example of the `useOptimistic` hook, a key feature in modern React.
 * It allows for updating the UI instantly, assuming a server action will succeed,
 * and automatically reverting if it fails.
 */

// Mock server action
async function updateUsernameOnServer(newName) {
  console.log("Sending new username to server:", newName);
  await new Promise(resolve => setTimeout(resolve, 1500)); // Simulate network delay
  if (newName.toLowerCase().includes("error")) {
    throw new Error("Server rejected the username.");
  }
  return newName;
}

function UsernameForm() {
  const [username, setUsername] = useState("user123");
  const [optimisticUsername, setOptimisticUsername] = useOptimistic(username);
  const [error, setError] = useState(null);

  const handleSubmit = async (event) => {
    event.preventDefault();
    const newUsername = event.target.elements.username.value;
    
    // Immediately update the UI with the new username
    setOptimisticUsername(newUsername);
    setError(null);

    try {
      // Send the update to the server
      const serverResponse = await updateUsernameOnServer(newUsername);
      // On success, commit the optimistic state
      setUsername(serverResponse);
    } catch (e) {
      // On failure, the optimistic update is automatically reverted.
      // We just need to handle the error state.
      setError(e.message);
      console.error(e);
    }
  };

  return (
    <div>
      <h1>Optimistic UI with `useOptimistic`</h1>
      <p>Current Username: <strong>{optimisticUsername}</strong></p>
      {optimisticUsername !== username && <small>(Updating...)</small>}
      
      <form onSubmit={handleSubmit}>
        <input type="text" name="username" defaultValue={username} />
        <button type="submit">Update Username</button>
      </form>
      
      {error && <p style={{ color: 'red' }}>Error: {error}</p>}
      <p><small>Try submitting "error" to see the automatic rollback.</small></p>
    </div>
  );
}

export default UsernameForm;
