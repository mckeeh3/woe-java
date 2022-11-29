fetch('https://green-voice-3640.us-east1.kalix.app/generator/generator2/create', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    generatorId: 'generator2', // removed the dash and it works
    position: {
      lat: '40.7128',
      lng: '-74.006',
    },
    radiusKm: 100,
    generate: 1000,
    ratePerSecond: 1000,
  }),
})
  .then((response) => {
    console.log(response);
  })
  .catch((err) => {
    console.error(err);
  });
